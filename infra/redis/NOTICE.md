# Redis 8 foundation license record

The project owner explicitly selected **AGPLv3** for official, unmodified Redis
8.4.7 in D1-08 Task B (2026-10-17). No Redis server source is forked or modified.
This selection does not relicense the independent E-Logistics application.

`LICENSE.txt` is the unmodified upstream 8.4.7 multi-license text, retained with
its copyright/notices. Redis permits selecting AGPLv3, RSALv2 or SSPLv1 for Redis8;
this project selects AGPLv3. Image pin: see `../compose/foundation.yaml`.

When conveying Redis binaries/images, retain license/copyright notices and meet
the applicable Corresponding Source requirements (AGPL section6); record the
exact source/build materials corresponding to the redistributed binary, including
other image components' licenses. Merely retaining a source URL is not a blanket
certification of distribution compliance. If Redis is modified and offered for
remote interaction, section13 requires offering the corresponding modified source
to those users. Review the actual distribution model before publishing images.
No Redis image redistribution or modified Redis service is performed by this task.

Source: https://raw.githubusercontent.com/redis/redis/8.4.7/LICENSE.txt
Overview: https://redis.io/legal/licenses/

Maintenance owner: repository/project owner; implementation maintainer records
version/digest, notices and source provenance before any redistribution or upgrade.
RabbitMQ4.3 community support currently ends2027-01-31; review the pin before
that date. Container and Java-client vulnerability certification remains separate.
