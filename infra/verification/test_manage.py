"""Negative safety controls. Real Compose/runtime verification is a separate gate."""
import copy
import importlib.util
import json
import os
from pathlib import Path
import secrets
import tempfile
import unittest
from unittest.mock import patch
import uuid
import sys

spec = importlib.util.spec_from_file_location('verification_manage', Path(__file__).with_name('manage.py'))
M = importlib.util.module_from_spec(spec); spec.loader.exec_module(M)
sys.path.insert(0, str(Path(__file__).parent))
import checks as C


def settings():
    s = M.identities(uuid.uuid4().hex)
    s.update(credentials={k: secrets.token_urlsafe(32) for k in
        ('bootstrap','migrator','runtime','redis','rabbit','token','admin','merchantA','merchantB','operator','disabled')}, rabbitSalt='AQIDBA==')
    return s


class IsolationPolicyTests(unittest.TestCase):
    def setUp(self):
        self.s = settings(); self.directory = M.ROOT / '.demo/verification' / self.s['session']
        self.images = M.approved_images()

    def config(self):
        s = self.s
        cfg = dict(name=s['project'], services={}, networks={'session': dict(name=s['network'], labels={M.LABEL:s['session']})},
            volumes={k:dict(name=s[v], labels={M.LABEL:s['session']}) for k,v in [('pg_data','pgVolume'),('rabbit_data','rabbitVolume')]}, secrets={})
        for key,name in [('pg_bootstrap','pg-bootstrap'),('pg_migrator','pg-migrator'),('pg_runtime','pg-runtime'),('redis_password','redis-password')]:
            cfg['secrets'][key]={'file':str(self.directory/name)}
        for service,port in M.PORTS.items():
            cfg['services'][service]=dict(image=self.images[service],platform='linux/amd64',labels={M.LABEL:s['session']},networks={'session':None},
                ports=[dict(host_ip='127.0.0.1',published='0',target=port)],
                volumes=[dict(type=t,source=src,target=dst,read_only=ro) for t,src,dst,ro in M.expected_mounts(self.directory)[service]])
        cfg['services']['postgres']['environment']=M.postgres_environment(s)
        cfg['services']['redis']['tmpfs']=['/data']
        cfg['services']['redis']['command']=['redis-server','/usr/local/etc/redis/redis.conf']
        cfg['services']['rabbitmq']['hostname']='verification-rabbit'
        for service,names in [('postgres',['pg_bootstrap','pg_migrator','pg_runtime']),('redis',['redis_password'])]:
            cfg['services'][service]['secrets']=[{'source':n,'target':'/run/secrets/'+n} for n in names]
        return cfg

    def validate(self, cfg): M.validate_effective(cfg,self.s,self.images,self.directory)

    def test_valid_config_has_three_random_loopback_ports(self): self.validate(self.config())

    def test_developer_container_identity_rejected(self):
        cfg=self.config();cfg['services']['postgres']['container_name']='elogistics-d1-postgres18'
        with self.assertRaises(RuntimeError):self.validate(cfg)

    def test_developer_volume_identity_rejected(self):
        cfg=self.config();cfg['volumes']['pg_data']['name']='elogistics_d1_pg18_dev_data'
        with self.assertRaises(RuntimeError):self.validate(cfg)

    def test_developer_database_rejected(self):
        cfg=self.config();cfg['services']['postgres']['environment']['VERIFY_DATABASE']='elogistics_dev'
        with self.assertRaises(RuntimeError):self.validate(cfg)

    def test_unexpected_project_and_external_network_rejected(self):
        for mutate in [lambda c:c.update(name='foreign'), lambda c:c['networks']['session'].update(external=True),
                       lambda c:c['networks']['session'].update(driver='host'),lambda c:c['volumes']['pg_data'].update(driver='foreign')]:
            cfg=self.config();mutate(cfg)
            with self.assertRaises(RuntimeError):self.validate(cfg)

    def test_image_digest_mutation_rejected(self):
        cfg=self.config();cfg['services']['redis']['image']='redis:latest'
        with self.assertRaises(RuntimeError):self.validate(cfg)

    def test_non_loopback_fixed_and_extra_ports_rejected(self):
        for changes in [{'host_ip':'0.0.0.0'},{'published':'5433'},{'target':9999}]:
            cfg=self.config();cfg['services']['postgres']['ports'][0].update(changes)
            with self.assertRaises(RuntimeError):self.validate(cfg)
        cfg=self.config();cfg['services']['rabbitmq']['ports'].append(dict(target=15672,published='0',host_ip='127.0.0.1'))
        with self.assertRaises(RuntimeError):self.validate(cfg)

    def test_developer_env_shadowing_and_unsafe_overrides_rejected(self):
        for key,value in [('env_file',['.env']),('privileged',True),('network_mode','host'),('entrypoint',['sh'])]:
            cfg=self.config();cfg['services']['postgres'][key]=value
            with self.assertRaises(RuntimeError):self.validate(cfg)

    def test_foreign_bind_and_writable_secret_rejected(self):
        cfg=self.config();cfg['services']['redis']['volumes'][0]['source']='/foreign/data'
        with self.assertRaises(RuntimeError):self.validate(cfg)

    def test_generated_secret_name_cannot_reference_another_project(self):
        cfg=self.config();cfg['secrets']['pg_runtime']['name']='foreign_pg_runtime'
        with self.assertRaises(RuntimeError):self.validate(cfg)

    def test_service_command_and_secret_target_mutations_rejected(self):
        cfg=self.config();cfg['services']['postgres']['command']=['sh','-c','synthetic']
        with self.assertRaises(RuntimeError):self.validate(cfg)
        cfg=self.config();cfg['services']['postgres']['secrets'][0]['target']='/foreign'
        with self.assertRaises(RuntimeError):self.validate(cfg)

    def test_compose_uses_explicit_empty_env_file_and_controlled_environment(self):
        session=M.Session.__new__(M.Session);session.s=self.s;session.images=self.images;session.directory=self.directory;session.executable='docker';session.context='local'
        with patch.dict(os.environ,{'PG_DB_URL':'synthetic-developer','COMPOSE_FILE':'foreign'}),patch.object(session,'command',return_value='{}') as command:
            session.compose('config','--format','json')
            args=command.call_args.args[0];env=command.call_args.kwargs['env']
            self.assertEqual(args[args.index('--env-file')+1],str(self.directory/'empty.env'))
            self.assertNotIn('PG_DB_URL',env);self.assertNotIn('COMPOSE_FILE',env)
            self.assertEqual(env['VERIFY_DATABASE'],self.s['database'])
        cfg=self.config();cfg['secrets']['pg_runtime']['file']=str(M.ROOT/'.demo/postgres/runtime-password.txt')
        with self.assertRaises(RuntimeError):self.validate(cfg)

    def test_inherited_connection_and_tool_redirection_rejected(self):
        for name in ['PG_DB_URL','SPRING_CONFIG_LOCATION','DOCKER_HOST','DOCKER_CONTEXT','COMPOSE_FILE','DEMO_PORT','FOUNDATION_REDIS_PORT','JAVA_TOOL_OPTIONS','VITE_API_URL']:
            with self.subTest(name=name),self.assertRaises(RuntimeError):M.guard_environment({name:'synthetic'})
        M.guard_environment({'PATH':'synthetic','JAVA_HOME':'synthetic'})

    def test_settings_identities_and_credential_corruption_rejected(self):
        M.validate_settings(self.s,self.s['session'])
        for key,value in [('database','elogistics_dev'),('pgVolume','elogistics_d1_pg18_dev_data'),('project','foreign')]:
            s=copy.deepcopy(self.s);s[key]=value
            with self.assertRaises(RuntimeError):M.validate_settings(s,self.s['session'])
        s=copy.deepcopy(self.s);s['credentials']['runtime']=''
        with self.assertRaises(RuntimeError):M.validate_settings(s,self.s['session'])

    def test_credentials_not_in_acl_or_rabbit_definitions(self):
        files=M.private_files(self.s)
        for key in ['redis','rabbit']:
            self.assertNotIn(self.s['credentials'][key],files['redis.acl']+files['definitions.json'])
        self.assertIn('user default off',files['redis.acl']);self.assertNotIn('+@all',files['redis.acl'])
        d=json.loads(files['definitions.json']);self.assertEqual(d['users'][0]['tags'],[])
        self.assertEqual({d['permissions'][0][k] for k in ['configure','read','write']},{'(?!)'})

    def test_pins_come_from_approved_sources_and_bootstrap_uses_safe_identifiers(self):
        for key,image in self.images.items():self.assertRegex(image,r'^'+key+r'@sha256:[a-f0-9]{64}$')
        sql=(M.ROOT/'infra/verification/bootstrap.sql').read_text()
        self.assertNotIn('elogistics_dev',sql);self.assertIn('CREATE DATABASE :"database"',sql)
        self.assertIn('NOLOGIN NOSUPERUSER NOCREATEDB NOCREATEROLE',sql)
        for value in self.s['credentials'].values():self.assertNotIn(value,sql)
        self.assertNotIn(b'\r',(M.ROOT/'infra/verification/bootstrap.sh').read_bytes())
        self.assertIn('*.sh text eol=lf',(M.ROOT/'infra/verification/.gitattributes').read_text())

    def test_duplicated_provisioning_keeps_existing_permission_invariants(self):
        old=(M.ROOT/'infra/postgres/bootstrap.sql').read_text()
        new=(M.ROOT/'infra/verification/bootstrap.sql').read_text()
        for fragment in ['NOLOGIN NOSUPERUSER NOCREATEDB NOCREATEROLE','LOGIN NOSUPERUSER NOCREATEDB NOCREATEROLE',
                         'REVOKE ALL ON SCHEMA public FROM PUBLIC;','CREATE SCHEMA platform AUTHORIZATION']:
            self.assertIn(fragment,old);self.assertIn(fragment,new)


class SessionLifecycleTests(unittest.TestCase):
    def setUp(self):
        self.session=M.Session.__new__(M.Session);self.session.s=settings()
        self.temp=tempfile.TemporaryDirectory(dir=M.ROOT/'.demo',prefix='verification-test-');self.addCleanup(self.temp.cleanup)
        self.session.directory=Path(self.temp.name)/self.session.s['session'];self.session.images=M.approved_images()
        self.session.context='local';self.session.executable='docker';self.calls=[]
        self.inventory=dict(containers=['a'*64],volumes=['developer-volume'],networks=['b'*64])
        self.state=dict(session=self.session.s['session'],daemon='daemon',phase='initialized',baseline=copy.deepcopy(self.inventory),containers={},volumes={},networks={})

    def test_missing_private_settings_never_falls_back(self):
        with self.assertRaisesRegex(RuntimeError,'Missing private'):self.session.load()

    def private_setup(self):
        self.session.directory.mkdir()
        self.session.save('settings.json',self.session.s)
        self.session.save('state.json',self.state)
        for name,value in M.private_files(self.session.s).items():(self.session.directory/name).write_text(value)

    def test_private_file_corruption_is_rejected_not_regenerated(self):
        self.private_setup();file=self.session.directory/'redis.acl';file.write_text('synthetic-tamper')
        with patch.object(self.session,'docker',return_value='daemon'),self.assertRaisesRegex(RuntimeError,'inconsistent'):self.session.load()
        self.assertEqual(file.read_text(),'synthetic-tamper')

    def test_missing_secret_is_rejected_without_recreation(self):
        self.private_setup();file=self.session.directory/'pg-runtime';file.unlink()
        with self.assertRaisesRegex(RuntimeError,'Missing/inconsistent'):self.session.load()
        self.assertFalse(file.exists())

    def test_repeated_init_preserves_all_private_bytes(self):
        self.private_setup();before={p.name:p.read_bytes() for p in self.session.directory.iterdir()}
        with patch.object(self.session,'docker',return_value='daemon'):
            self.assertEqual(self.session.init(),self.session.s['session'])
        self.assertEqual(before,{p.name:p.read_bytes() for p in self.session.directory.iterdir()})

    def test_daemon_change_rejected(self):
        self.private_setup()
        with patch.object(self.session,'docker',return_value='foreign-daemon'),self.assertRaisesRegex(RuntimeError,'daemon'):self.session.load()

    def test_foreign_label_rejects_before_cleanup(self):
        item=dict(Labels={M.LABEL:'foreign','com.docker.compose.project':self.session.s['project']})
        with self.assertRaisesRegex(RuntimeError,'Foreign'):self.session.label(item)

    def test_cleanup_refuses_questionable_ownership_before_any_removal(self):
        with patch.object(self.session,'load',return_value=self.state),patch.object(self.session,'discover',side_effect=RuntimeError('Foreign ownership')),patch.object(self.session,'docker') as docker:
            with self.assertRaises(RuntimeError):self.session.down()
            docker.assert_not_called()

    def test_cleanup_only_captured_ids_preserves_foreign_inventory_and_is_repeatable(self):
        owned=copy.deepcopy(self.state);owned.update(containers={'postgres':'c'*64},volumes={'pg':'owned-volume'},networks={'session':'d'*64})
        with patch.object(self.session,'load',side_effect=[owned,self.state]),patch.object(self.session,'discover',side_effect=lambda s:s),patch.object(self.session,'inventory',return_value=self.inventory),patch.object(self.session,'save'),patch.object(self.session,'docker') as docker:
            self.assertTrue(self.session.down()['baselinePreserved']);self.assertTrue(self.session.down()['inventoryEqualsBaseline'])
            self.assertEqual([c.args for c in docker.call_args_list],[('rm','-f','c'*64),('volume','rm','owned-volume'),('network','rm','d'*64)])

    def test_interrupted_start_records_ownership_and_does_not_run_cleanup(self):
        with patch.object(self.session,'load',return_value=self.state),patch.object(self.session,'dry_run'),patch.object(self.session,'reject_collisions'),patch.object(self.session,'save'),patch.object(self.session,'docker',return_value=json.dumps([dict(RepoDigests=list(self.session.images.values()),Os='linux',Architecture='amd64')])),patch.object(self.session,'compose',side_effect=RuntimeError('interrupted')),patch.object(self.session,'discover') as discover:
            with self.assertRaisesRegex(RuntimeError,'interrupted'):self.session.up()
            discover.assert_called_once_with(self.state)
            self.assertEqual(self.state['phase'],'starting')

    def test_ready_up_does_not_recreate_resources(self):
        state=dict(self.state,phase='ready')
        with patch.object(self.session,'load',return_value=state),patch.object(self.session,'dry_run'),patch.object(self.session,'status',return_value={'owned':True}),patch.object(self.session,'compose') as compose:
            self.assertEqual(self.session.up(),{'owned':True});compose.assert_not_called()

    def test_new_session_rejects_existing_volume_before_creating_private_files(self):
        inv=dict(self.inventory,volumes=[self.session.s['pgVolume']])
        with patch.object(self.session,'inventory',return_value=inv):
            with self.assertRaisesRegex(RuntimeError,'Volume collision'):self.session.init()
        self.assertFalse(self.session.directory.exists())

    def test_new_session_rejects_existing_container_before_writes(self):
        item=dict(Name='/'+self.session.s['project']+'-postgres-1',Config={'Labels':{}})
        with patch.object(self.session,'inventory',return_value=self.inventory),patch.object(self.session,'docker',return_value=json.dumps([item])):
            with self.assertRaisesRegex(RuntimeError,'Container'):self.session.init()
        self.assertFalse(self.session.directory.exists())

    def test_dry_run_only_resolves_config_and_does_not_start_resources(self):
        with patch.object(self.session,'load'),patch.object(self.session,'compose',return_value='{}') as compose,patch.object(M,'validate_effective'),patch.object(self.session,'save'):
            self.session.dry_run();compose.assert_called_once_with('config','--format','json')

    def test_dry_run_rejects_new_collision_before_compose(self):
        with patch.object(self.session,'load',return_value=self.state),patch.object(self.session,'reject_collisions',side_effect=RuntimeError('collision')),patch.object(self.session,'compose') as compose:
            with self.assertRaisesRegex(RuntimeError,'collision'):self.session.dry_run()
            compose.assert_not_called()

    def test_cleanup_stops_before_removals_when_foreign_inventory_changed(self):
        with patch.object(self.session,'load',return_value=self.state),patch.object(self.session,'discover',side_effect=lambda x:x),patch.object(self.session,'inventory',return_value=dict(containers=[],volumes=[],networks=[])),patch.object(self.session,'docker') as docker:
            with self.assertRaisesRegex(RuntimeError,'before cleanup'):self.session.down()
            docker.assert_not_called()

    def test_status_rejects_wrong_live_binding(self):
        state=dict(self.state,containers={k:k[0]*64 for k in M.PORTS},volumes={'pg':'p','rabbit':'r'},networks={'session':'n'})
        info=dict(State=dict(Running=True,Health={'Status':'healthy'}),NetworkSettings=dict(Ports={'5432/tcp':[dict(HostIp='0.0.0.0',HostPort='5433')]},Networks={self.session.s['network']: {}}))
        with patch.object(self.session,'load',return_value=state),patch.object(self.session,'discover',return_value=state),patch.object(self.session,'docker',return_value=json.dumps([info])),self.assertRaisesRegex(RuntimeError,'loopback'):self.session.status()

    def test_runtime_mount_rejects_developer_volume(self):
        info=dict(Mounts=[dict(Type='volume',Name='elogistics_d1_pg18_dev_data',Destination='/var/lib/postgresql',RW=True)],HostConfig={})
        with self.assertRaisesRegex(RuntimeError,'actual mounts'):self.session.verify_mounts('postgres',info)

    def test_plaintext_credential_and_jwt_in_diagnostics_are_rejected(self):
        path=Path(self.temp.name)/'diagnostic.txt'
        # Deliberately invalid token, assembled at runtime: no stored token literal.
        synthetic_token = 'eyJ' + 'synthetic' + '.' + 'payload' + '.' + 'signature'
        for text in [self.session.s['credentials']['runtime'],synthetic_token]:
            path.write_text(text)
            with self.assertRaises(RuntimeError):C.privacy(self.session,[path])
        path.write_text('safe status-only diagnostic');C.privacy(self.session,[path])

    def test_backend_connections_are_derived_only_from_live_owned_bindings(self):
        info=dict(bindings=dict(postgres=49191,redis=49192,rabbitmq=49193))
        with patch.dict(os.environ,{'PG_DB_URL':'synthetic-developer','SPRING_CONFIG_LOCATION':'foreign'}),patch.object(self.session,'status',return_value=info):
            env=C.application_environment(self.session)
        self.assertEqual(env['PG_DB_URL'],f'jdbc:postgresql://127.0.0.1:49191/{self.session.s["database"]}')
        self.assertNotIn('SPRING_CONFIG_LOCATION',env)
        self.assertEqual(env['FOUNDATION_REDIS_PORT'],'49192');self.assertEqual(env['FOUNDATION_RABBIT_PORT'],'49193')

    def test_actual_discovery_rejects_foreign_network_attachment_before_removal(self):
        inv=dict(containers=[],volumes=[],networks=['n'*64])
        network=dict(Name=self.session.s['network'],Id='n'*64,Labels={M.LABEL:self.session.s['session'],'com.docker.compose.project':self.session.s['project']},Containers={'foreign-container':{}})
        with patch.object(self.session,'load',return_value=self.state),patch.object(self.session,'inventory',return_value=inv),patch.object(self.session,'docker',return_value=json.dumps([network])) as docker:
            with self.assertRaisesRegex(RuntimeError,'Foreign network'):self.session.down()
            self.assertTrue(all(c.args[:2]==('network','inspect') for c in docker.call_args_list))

    def test_actual_discovery_rejects_foreign_volume_consumer_before_removal(self):
        inv=dict(containers=[],volumes=[self.session.s['pgVolume']],networks=[])
        volume=dict(Labels={M.LABEL:self.session.s['session'],'com.docker.compose.project':self.session.s['project']},Options={})
        with patch.object(self.session,'load',return_value=self.state),patch.object(self.session,'inventory',return_value=inv),patch.object(self.session,'docker',side_effect=[json.dumps([volume]),'foreign-container']) as docker:
            with self.assertRaisesRegex(RuntimeError,'Foreign volume'):self.session.down()
            self.assertTrue(all(c.args[0] in ['volume','ps'] and 'rm' not in c.args for c in docker.call_args_list))

    def test_actual_discovery_rejects_runtime_image_mutation(self):
        inv=dict(containers=['c'*64],volumes=[],networks=[])
        item=dict(Name='/'+self.session.s['project']+'-postgres-1',Config=dict(Image='postgres:latest',Labels={M.LABEL:self.session.s['session'],'com.docker.compose.project':self.session.s['project'],'com.docker.compose.service':'postgres'}),HostConfig={})
        with patch.object(self.session,'inventory',return_value=inv),patch.object(self.session,'docker',return_value=json.dumps([item])):
            with self.assertRaisesRegex(RuntimeError,'Unsafe actual container'):self.session.discover(self.state)

    def test_remote_docker_tcp_and_remote_named_pipe_are_rejected(self):
        for endpoint in ['tcp://127.0.0.1:2375','npipe:////foreign-host/pipe/docker']:
            with patch.object(M,'guard_environment'),patch.object(M.Session,'command',side_effect=['synthetic-context',json.dumps([{'Endpoints':{'docker':{'Host':endpoint}}}])]):
                with self.assertRaisesRegex(RuntimeError,'Non-local Docker'):M.Session(uuid.uuid4().hex)


if __name__=='__main__':unittest.main()
