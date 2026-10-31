import { useEffect, useState } from 'react';
import { Link, NavLink, useLocation } from 'react-router-dom';
import './public.css';

type Utility = 'tracking' | 'quote' | 'coverage' | 'stations' | 'faq';
const utilities: { id: Utility; title: string; description: string; path: string; icon: string }[] = [
  { id: 'tracking', title: 'Tra cứu vận đơn', description: 'Theo dõi hành trình khi dịch vụ tra cứu công khai được kích hoạt.', path: '/tracking', icon: 'route' },
  { id: 'quote', title: 'Ước tính cước', description: 'Bảng giá và điều kiện dịch vụ đang được hoàn thiện.', path: '/quote', icon: 'box' },
  { id: 'coverage', title: 'Vùng phục vụ', description: 'Danh mục địa danh và vùng phục vụ đang được chuẩn bị.', path: '/coverage', icon: 'pin' },
  { id: 'stations', title: 'Tìm bưu cục', description: 'Thông tin Station và Hub sẽ được công bố sau khi xác nhận.', path: '/stations', icon: 'station' },
  { id: 'faq', title: 'Câu hỏi thường gặp', description: 'Tìm hiểu về tài khoản, xác minh email và hành trình gửi hàng.', path: '/faq', icon: 'help' },
];

function Glyph({ name }: { name: string }) {
  const paths: Record<string, string> = {
    route: 'M4 17h4l3-5 4 3 5-8M4 17a2 2 0 1 0 0 .01M20 7a2 2 0 1 0 0 .01',
    box: 'm3 7 9-4 9 4-9 4-9-4Zm0 0v10l9 4 9-4V7M12 11v10',
    pin: 'M12 21s7-6 7-12a7 7 0 0 0-14 0c0 6 7 12 7 12Zm0-9a3 3 0 1 0 0-6 3 3 0 0 0 0 6Z',
    station: 'M4 21V7l8-4 8 4v14M4 11h16M8 21v-6h8v6M2 21h20',
    help: 'M9 9a3 3 0 0 1 6 0c0 2-3 2-3 5m0 3h.01M12 22a10 10 0 1 0 0-20 10 10 0 0 0 0 20Z',
    arrow: 'M4 12h16m-6-6 6 6-6 6',
    check: 'm4 12 5 5L20 6',
    user: 'M20 21v-2a7 7 0 0 0-14 0v2M13 11a4 4 0 1 0 0-8 4 4 0 0 0 0 8Z',
  };
  return <svg aria-hidden="true" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.8" strokeLinecap="round" strokeLinejoin="round"><path d={paths[name] ?? paths.box} /></svg>;
}

export function PublicBrand() {
  return <Link className="ps-brand" to="/" aria-label="movapost., về trang chủ"><span className="ps-wordmark">mova<span>post.</span></span></Link>;
}

const nav = [
  { label: 'Trang chủ', path: '/' }, { label: 'Dịch vụ', path: '/services' },
  { label: 'Tiện ích', path: '/tracking' }, { label: 'Tin & hướng dẫn', path: '/news' },
  { label: 'Về chúng tôi', path: '/about' }, { label: 'Hỗ trợ', path: '/support' },
];

export function PublicHeader({ compact = false }: { compact?: boolean }) {
  const [open, setOpen] = useState(false);
  const location = useLocation();
  return <header className={`ps-header ${compact ? 'ps-header-compact' : ''}`}>
    <div className="ps-header-inner"><PublicBrand />
      {!compact && <><button className="ps-menu-button" type="button" aria-label={open ? 'Đóng menu' : 'Mở menu'} aria-expanded={open} aria-controls="ps-primary-nav" onClick={() => setOpen(value => !value)}><span /><span /><span /></button>
        <nav id="ps-primary-nav" className={`ps-nav ${open ? 'is-open' : ''}`} aria-label="Điều hướng chính">{nav.map(item => <NavLink key={item.path} to={item.path} end={item.path === '/'} onClick={() => setOpen(false)} className={({ isActive }) => isActive || item.path === '/tracking' && utilities.some(utility => location.pathname === utility.path) ? 'is-active' : ''}>{item.label}</NavLink>)}</nav></>}
      {compact ? <nav className="ps-compact-links" aria-label="Điều hướng hỗ trợ"><Link to="/services">Dịch vụ</Link><Link to="/support">Hỗ trợ khách hàng</Link></nav>
        : <div className="ps-account-links"><Link className="ps-signup" to="/login"><Glyph name="user" /><span>ĐĂNG NHẬP / ĐĂNG KÝ</span></Link></div>}
    </div>
  </header>;
}

export function PublicFooter() {
  return <footer className="ps-footer"><div className="ps-footer-inner">
    <div className="ps-footer-lead"><PublicBrand /><p>Gửi niềm tin, nối hành trình. Một không gian để người gửi bắt đầu và quản lý thông tin của mình.</p><span className="ps-footer-note">Dịch vụ vận chuyển và các tiện ích tra cứu sẽ được mở khi sẵn sàng.</span></div>
    <div><h2>Khám phá</h2><Link to="/services">Dịch vụ</Link><Link to="/about">Giới thiệu</Link><Link to="/news">Tin & hướng dẫn</Link><Link to="/stations">Mạng lưới bưu cục</Link></div>
    <div><h2>Hỗ trợ</h2><Link to="/tracking">Tra cứu vận đơn</Link><Link to="/quote">Ước tính cước</Link><Link to="/coverage">Vùng phục vụ</Link><Link to="/faq">Câu hỏi thường gặp</Link><Link to="/support">Liên hệ</Link></div>
    <div><h2>Tài khoản & chính sách</h2><Link to="/register">Tạo tài khoản cá nhân</Link><Link to="/login">Đăng nhập</Link><Link to="/recover">Khôi phục mật khẩu</Link><Link to="/privacy">Bảo mật dữ liệu</Link><Link to="/terms">Điều khoản sử dụng</Link><Link to="/shipping-policy">Chính sách vận chuyển</Link></div>
  </div><div className="ps-footer-bottom"><span>© {new Date().getFullYear()} movapost.</span><span>Gửi niềm tin, nối hành trình.</span></div></footer>;
}

function SectionHeading({ eyebrow, title, description }: { eyebrow: string; title: string; description?: string }) {
  return <div className="ps-section-heading"><span className="ps-eyebrow">{eyebrow}</span><h2>{title}</h2>{description && <p>{description}</p>}</div>;
}

function Home() {
  const [selected, setSelected] = useState<Utility>('tracking');
  const utility = utilities.find(item => item.id === selected)!;
  return <>
    <section className="ps-hero"><div className="ps-container ps-hero-grid"><div className="ps-hero-copy"><span className="ps-hero-kicker"><span /> GỬI NIỀM TIN, NỐI HÀNH TRÌNH</span><h1>Mỗi kiện hàng,<br />một hành trình<br /><em>được kết nối.</em></h1><p>Bắt đầu từ tài khoản của bạn. movapost. kết nối người gửi với hành trình chuyển phát trong một trải nghiệm rõ ràng, dễ sử dụng.</p><div className="ps-hero-actions"><Link className="ps-button ps-button-orange" to="/register">Tạo tài khoản <Glyph name="arrow" /></Link><Link className="ps-button ps-button-outline" to="/about">Khám phá movapost.</Link></div><div className="ps-hero-foot"><span><Glyph name="check" /> Xác minh email an toàn</span><span><Glyph name="check" /> Quản lý hồ sơ cá nhân</span></div></div><div className="ps-hero-art"><div className="ps-hero-orbit" /><img src="/media/movapost-network.svg" alt="Minh họa các kiện hàng kết nối qua mạng lưới chuyển phát movapost." width="660" height="540" /><div className="ps-hero-caption"><span><Glyph name="box" /></span><div><strong>Kết nối từng chặng</strong><small>Đồng hành cùng người gửi</small></div><Glyph name="check" /></div></div></div><div className="ps-hero-bottom" aria-hidden="true">mova<span>post.</span></div></section>
    <section className="ps-utilities ps-container ps-reveal" aria-labelledby="ps-utilities-title"><div className="ps-utility-intro"><div><span className="ps-eyebrow">TIỆN ÍCH CHUYỂN PHÁT</span><h2 id="ps-utilities-title">Bạn cần tìm gì?</h2></div><Link className="ps-text-link" to="/faq">Trung tâm trợ giúp <Glyph name="arrow" /></Link></div><div className="ps-utility-box"><div className="ps-utility-tabs" role="tablist" aria-label="Tiện ích công khai">{utilities.map((item, index) => <button key={item.id} type="button" role="tab" id={`tab-${item.id}`} tabIndex={selected === item.id ? 0 : -1} aria-selected={selected === item.id} aria-controls="ps-utility-panel" className={selected === item.id ? 'is-selected' : ''} onClick={() => setSelected(item.id)} onKeyDown={event => { if (!['ArrowRight', 'ArrowLeft', 'Home', 'End'].includes(event.key)) return; event.preventDefault(); const next = event.key === 'Home' ? 0 : event.key === 'End' ? utilities.length - 1 : (index + (event.key === 'ArrowRight' ? 1 : -1) + utilities.length) % utilities.length; setSelected(utilities[next].id); document.getElementById(`tab-${utilities[next].id}`)?.focus(); }}><Glyph name={item.icon} /><span>{item.title}</span></button>)}</div><div className="ps-utility-panel" key={selected} id="ps-utility-panel" role="tabpanel" aria-labelledby={`tab-${selected}`}><div><span className="ps-status">{selected === 'faq' ? 'HƯỚNG DẪN TÀI KHOẢN' : 'SẮP RA MẮT'}</span><h3>{utility.title}</h3><p>{utility.description}</p><Link className="ps-text-link" to={utility.path}>{selected === 'faq' ? 'Xem câu hỏi thường gặp' : 'Xem thông tin tiện ích'} <Glyph name="arrow" /></Link></div><div className="ps-utility-visual" aria-hidden="true"><Glyph name={utility.icon} /><span className="ps-utility-orbit one" /><span className="ps-utility-orbit two" /></div></div></div></section>
    <section className="ps-section ps-services ps-reveal"><div className="ps-container"><SectionHeading eyebrow="BẮT ĐẦU THẬT DỄ DÀNG" title="Không gian của người gửi." description="Những bước cần thiết, trong một trải nghiệm liền mạch." /><div className="ps-service-grid">{[{icon:'user',number:'01',title:'Một tài khoản của bạn',text:'Đăng ký bằng email, xác minh với mã OTP và đăng nhập an toàn.',path:'/register',link:'Tạo tài khoản'},{icon:'box',number:'02',title:'Hồ sơ luôn trong tầm tay',text:'Cập nhật thông tin cá nhân và thông tin liên hệ trong không gian riêng.',path:'/personal/profile',link:'Quản lý hồ sơ'},{icon:'station',number:'03',title:'Sẵn sàng cho nhà bán',text:'Từ tài khoản đã xác minh, chuẩn bị hồ sơ Merchant khi bạn cần.',path:'/personal/applications',link:'Khám phá hồ sơ Merchant'}].map(item => <article className="ps-service-card" key={item.number}><div className="ps-service-card-top"><span className="ps-service-icon"><Glyph name={item.icon} /></span><span className="ps-service-number">{item.number}</span></div><h3>{item.title}</h3><p>{item.text}</p><Link to={item.path}>{item.link} <Glyph name="arrow" /></Link></article>)}</div></div></section>
    <section className="ps-section ps-process ps-reveal"><div className="ps-container ps-process-grid"><div><SectionHeading eyebrow="MỌI THỨ RÕ RÀNG HƠN" title="Tập trung vào điều bạn cần." /><p>Tài khoản được bảo vệ bằng email đã xác minh. Thông tin cá nhân và quyền truy cập luôn nằm trong không gian của bạn.</p><Link className="ps-text-link" to="/faq">Xem hướng dẫn tài khoản <Glyph name="arrow" /></Link></div><div className="ps-home-faq"><details open><summary>Tạo tài khoản như thế nào?</summary><p>Nhập thông tin đăng ký, nhận OTP qua email và nhập mã ngay trên website trong 2 phút.</p></details><details><summary>Không nhận được mã xác minh?</summary><p>Kiểm tra thư rác và địa chỉ email, sau đó chọn gửi lại mã trên màn hình xác minh. Mỗi mã mới thay thế mã trước.</p></details><details><summary>Quên mật khẩu thì làm sao?</summary><p>Chọn <Link to="/recover">Quên mật khẩu?</Link>, xác minh email bằng OTP và đặt mật khẩu mới.</p></details></div></div></section>
    <section className="ps-end-cta ps-reveal"><div className="ps-container ps-end-cta-inner"><div><span className="ps-eyebrow">CÙNG MOVAPOST.</span><h2>Bắt đầu kết nối, ngay hôm nay.</h2><p>Tài khoản cá nhân của bạn là bước đầu tiên.</p></div><Link className="ps-button ps-button-orange" to="/register">Đăng ký tài khoản <Glyph name="arrow" /></Link></div></section>
  </>;
}

const pages: Record<string, { eyebrow: string; title: string; description: string; body: string; coming?: boolean }> = {
  '/services': { eyebrow: 'DỊCH VỤ', title: 'Dịch vụ chuyển phát', description: 'Hành trình dành cho người gửi cá nhân và doanh nghiệp.', body: 'movapost. đang phát triển các luồng gửi hàng, lấy hàng, phân loại, vận chuyển và giao nhận trên cùng nền tảng. Bảng giá, vùng phục vụ và thời gian cam kết sẽ được công bố sau khi cấu hình vận hành được xác nhận.', coming: true },
  '/tracking': { eyebrow: 'TIỆN ÍCH', title: 'Tra cứu vận đơn', description: 'Tính năng tra cứu công khai đang được phát triển.', body: 'Tiện ích tra cứu đang được chuẩn bị. Vui lòng quay lại khi dịch vụ được mở.', coming: true },
  '/quote': { eyebrow: 'TIỆN ÍCH', title: 'Ước tính cước phí', description: 'Báo giá chỉ có giá trị khi chính sách giá được xác nhận.', body: 'Công cụ ước tính cước và bảng giá chưa được công bố. movapost. sẽ hiển thị kết quả khi có chính sách dịch vụ và dữ liệu hợp lệ.', coming: true },
  '/coverage': { eyebrow: 'TIỆN ÍCH', title: 'Vùng phục vụ', description: 'Danh mục địa danh đang được chuẩn bị.', body: 'Thông tin vùng phục vụ cần được đối chiếu với cấu hình Station, Hub và tuyến vận hành trước khi cung cấp kết quả tra cứu.', coming: true },
  '/stations': { eyebrow: 'MẠNG LƯỚI', title: 'Tìm bưu cục', description: 'Các điểm Station và Hub sẽ được hiển thị khi đã có dữ liệu chính thức.', body: 'movapost. chưa công bố địa chỉ bưu cục. Không sử dụng địa điểm minh họa để hướng dẫn gửi hàng thực tế.', coming: true },
  '/about': { eyebrow: 'VỀ MOVAPOST.', title: 'Một mạng lưới chuyển phát có kết nối', description: 'Nền tảng quản lý chuyển phát cho người gửi, Merchant và đội vận hành.', body: 'Mô hình movapost. kết nối pickup, Station, Hub, phân loại, tuyến vận chuyển và giao nhận. Mỗi chặng có quyền thao tác và dữ liệu riêng, cùng đóng góp vào một hành trình mà khách hàng có thể theo dõi khi các dịch vụ được triển khai.' },
  '/support': { eyebrow: 'HỖ TRỢ', title: 'Hỗ trợ khách hàng', description: 'Tìm câu trả lời và theo dõi các kênh hỗ trợ chính thức.', body: 'Câu hỏi về tài khoản có thể xem trong mục FAQ. Các kênh liên hệ chính thức sẽ được cập nhật tại đây khi sẵn sàng.' },
  '/privacy': { eyebrow: 'CHÍNH SÁCH', title: 'Bảo mật dữ liệu', description: 'Nội dung chính sách chính thức đang được chuẩn bị.', body: 'Trang này là vị trí dành cho chính sách bảo vệ dữ liệu sau khi được phê duyệt. Chưa sử dụng nội dung này làm văn bản pháp lý hoặc bằng chứng đồng ý chính sách.', coming: true },
  '/terms': { eyebrow: 'CHÍNH SÁCH', title: 'Điều khoản sử dụng', description: 'Điều khoản chính thức đang được chuẩn bị.', body: 'Các điều kiện dịch vụ và điều khoản sử dụng sẽ được công bố sau khi được phê duyệt. Hiện chưa có cam kết thương mại từ trang này.', coming: true },
  '/shipping-policy': { eyebrow: 'CHÍNH SÁCH', title: 'Chính sách vận chuyển', description: 'Chính sách vận chuyển đang được hoàn thiện.', body: 'Thông tin về vùng phục vụ, thời gian, phí, hàng hạn chế và xử lý sự cố sẽ được công bố cùng dịch vụ vận chuyển chính thức.', coming: true },
};

const guides = [
  { category: 'Hướng dẫn', title: 'Tạo và xác minh tài khoản cá nhân', text: 'Bắt đầu bằng email của bạn. Sau khi xác minh, bạn có thể đăng nhập và quản lý hồ sơ cá nhân.' },
  { category: 'Nền tảng', title: 'Đăng ký trở thành Merchant diễn ra thế nào?', text: 'Tài khoản cá nhân có thể chuẩn bị bản nháp; quyền tổ chức chỉ có sau bước xét duyệt hợp lệ.' },
  { category: 'Hướng dẫn', title: 'Khôi phục quyền truy cập tài khoản', text: 'Yêu cầu hướng dẫn qua email đã xác minh và xác minh OTP rồi đặt mật khẩu mới.' },
];

function News() {
  const [filter, setFilter] = useState('Tất cả');
  return <section className="ps-inner-page ps-container"><span className="ps-eyebrow">GÓC THÔNG TIN</span><h1>Cập nhật & hướng dẫn</h1><p className="ps-inner-lead">Thông tin giúp bạn sử dụng movapost. dễ dàng hơn.</p><div className="ps-filters" aria-label="Lọc nội dung">{['Tất cả', 'Hướng dẫn', 'Nền tảng'].map(item => <button key={item} type="button" aria-pressed={filter === item} onClick={() => setFilter(item)}>{item}</button>)}</div><div className="ps-guide-grid">{guides.filter(item => filter === 'Tất cả' || filter === item.category).map(item => <article key={item.title}><span className="ps-tag">{item.category.toUpperCase()}</span><h2>{item.title}</h2><p>{item.text}</p></article>)}</div></section>;
}

function Faq() {
  return <section className="ps-inner-page ps-container"><span className="ps-eyebrow">HỖ TRỢ</span><h1>Câu hỏi thường gặp</h1><p className="ps-inner-lead">Những bước tài khoản hiện đã có trên hệ thống.</p><div className="ps-faq-list"><details><summary>Tôi cần gì để tạo tài khoản?</summary><p>Bạn cần họ tên hiển thị, địa chỉ email, số điện thoại liên hệ (có thể bắt đầu bằng 0) và mật khẩu hợp lệ. Email phải được xác minh trước khi đăng nhập; số điện thoại hiện chỉ là liên hệ chưa xác minh.</p></details><details><summary>Quên mật khẩu thì làm thế nào?</summary><p>Vào trang <Link to="/recover">khôi phục mật khẩu</Link>, nhập email đã xác minh và làm theo hướng dẫn trong thư. Hệ thống không tiết lộ địa chỉ nào đã đăng ký.</p></details><details><summary>Tạo tài khoản có tự cấp quyền Merchant không?</summary><p>Không. Tài khoản cá nhân có thể chuẩn bị hồ sơ Merchant; quyền tổ chức cần quy trình xét duyệt và cấp quyền riêng.</p></details><details><summary>Tôi đã có thể tra cứu vận đơn và xem giá chưa?</summary><p>Hai tiện ích này đang được phát triển và hiện chưa trả kết quả chính thức.</p></details></div></section>;
}

function InfoPage({ path }: { path: string }) {
  const page = pages[path];
  if (!page) return <section className="ps-inner-page ps-container"><span className="ps-eyebrow">KHÔNG TÌM THẤY</span><h1>Trang bạn tìm chưa có</h1><p className="ps-inner-lead">Đường dẫn này không thuộc website hiện tại.</p><Link className="ps-button ps-button-dark" to="/">Về trang chủ</Link></section>;
  return <section className="ps-inner-page ps-container"><span className="ps-eyebrow">{page.eyebrow}</span><h1>{page.title}</h1><p className="ps-inner-lead">{page.description}</p><div className="ps-info-grid"><article><span className="ps-status">{page.coming ? 'ĐANG PHÁT TRIỂN' : 'GIỚI THIỆU'}</span><p>{page.body}</p><div className="ps-info-actions"><Link className="ps-button ps-button-dark" to="/">Về trang chủ <Glyph name="arrow" /></Link><Link className="ps-text-link" to="/faq">Xem FAQ</Link></div></article><aside><Glyph name={path === '/stations' ? 'station' : path === '/coverage' ? 'pin' : 'route'} /><strong>Rõ thông tin ở từng bước</strong><p>Chúng tôi chỉ hiển thị dữ liệu dịch vụ khi nguồn và điều kiện vận hành đã được xác nhận.</p></aside></div></section>;
}

export default function PublicWebsite() {
  const { pathname } = useLocation();
  useEffect(() => {
    if (!('IntersectionObserver' in window)) return;
    const observer = new IntersectionObserver(entries => { for (const entry of entries) if (entry.isIntersecting) { entry.target.classList.add('is-visible'); observer.unobserve(entry.target); } }, { threshold: 0.08 });
    document.querySelectorAll('.ps-reveal').forEach(element => { element.classList.add('will-reveal'); observer.observe(element); });
    return () => observer.disconnect();
  }, [pathname]);
  return <div className="public-site"><a className="ps-skip" href="#public-main">Đến nội dung chính</a><PublicHeader /><main id="public-main" key={pathname}>{pathname === '/' ? <Home /> : pathname === '/news' ? <News /> : pathname === '/faq' ? <Faq /> : <InfoPage path={pathname} />}</main><PublicFooter /></div>;
}
