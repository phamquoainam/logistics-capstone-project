import { useState } from 'react';
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
  };
  return <svg aria-hidden="true" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.8" strokeLinecap="round" strokeLinejoin="round"><path d={paths[name] ?? paths.box} /></svg>;
}

export function PublicBrand() {
  return <Link className="ps-brand" to="/" aria-label="E-Logistics, về trang chủ"><span className="ps-brand-symbol">E<span>↗</span></span><span className="ps-brand-word">E-Logistics<small>Chuyển phát có kết nối</small></span></Link>;
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
      <div className="ps-account-links"><Link className="ps-signin" to="/login">Đăng nhập</Link><Link className="ps-signup" to="/register">Tạo tài khoản <Glyph name="arrow" /></Link></div>
    </div>
  </header>;
}

export function PublicFooter() {
  return <footer className="ps-footer"><div className="ps-footer-inner">
    <div className="ps-footer-lead"><PublicBrand /><p>E-Logistics xây dựng trải nghiệm chuyển phát kết nối người gửi, mạng lưới Station/Hub và đội giao nhận trong một hành trình minh bạch.</p><span className="ps-footer-note">Website giới thiệu sản phẩm đang phát triển. Thông tin dịch vụ chính thức sẽ được công bố khi sẵn sàng.</span></div>
    <div><h2>Khám phá</h2><Link to="/services">Dịch vụ</Link><Link to="/about">Giới thiệu</Link><Link to="/news">Tin & hướng dẫn</Link><Link to="/stations">Mạng lưới bưu cục</Link></div>
    <div><h2>Hỗ trợ</h2><Link to="/tracking">Tra cứu vận đơn</Link><Link to="/quote">Ước tính cước</Link><Link to="/coverage">Vùng phục vụ</Link><Link to="/faq">Câu hỏi thường gặp</Link><Link to="/support">Liên hệ</Link></div>
    <div><h2>Tài khoản & chính sách</h2><Link to="/register">Tạo tài khoản cá nhân</Link><Link to="/login">Đăng nhập</Link><Link to="/recover">Khôi phục mật khẩu</Link><Link to="/privacy">Bảo mật dữ liệu</Link><Link to="/terms">Điều khoản sử dụng</Link><Link to="/shipping-policy">Chính sách vận chuyển</Link></div>
  </div><div className="ps-footer-bottom"><span>© {new Date().getFullYear()} E-Logistics · Dự án capstone</span><span>Thông tin liên hệ chính thức: đang cập nhật</span></div></footer>;
}

function SectionHeading({ eyebrow, title, description }: { eyebrow: string; title: string; description?: string }) {
  return <div className="ps-section-heading"><span className="ps-eyebrow">{eyebrow}</span><h2>{title}</h2>{description && <p>{description}</p>}</div>;
}

function Home() {
  const [selected, setSelected] = useState<Utility>('tracking');
  const utility = utilities.find(item => item.id === selected)!;
  return <>
    <section className="ps-hero"><div className="ps-container ps-hero-grid"><div className="ps-hero-copy"><span className="ps-hero-kicker"><span /> NỀN TẢNG CHUYỂN PHÁT E-LOGISTICS</span><h1>Chuyển phát rõ từng chặng.<br /><em>Kết nối trọn hành trình.</em></h1><p>Từ lúc người gửi chuẩn bị kiện hàng đến khi giao nhận hoàn tất, E-Logistics hướng tới một trải nghiệm dễ theo dõi cho khách hàng và mạng lưới vận hành.</p><div className="ps-hero-actions"><Link className="ps-button ps-button-light" to="/register">Bắt đầu với tài khoản cá nhân <Glyph name="arrow" /></Link><Link className="ps-button ps-button-outline" to="/services">Khám phá dịch vụ</Link></div><div className="ps-hero-foot"><span><Glyph name="check" /> Tài khoản email đã xác minh</span><span><Glyph name="check" /> Một nơi cho hành trình gửi hàng</span></div></div><div className="ps-hero-art"><img src="/api/v1/public-media/hero-network.svg" onError={event => { if (!event.currentTarget.src.endsWith('/media/hero-network.svg')) event.currentTarget.src = '/media/hero-network.svg'; }} alt="Minh họa các kiện hàng di chuyển qua trạm phân loại và tuyến giao nhận" width="680" height="520" /></div></div></section>
    <section className="ps-utilities ps-container" aria-labelledby="ps-utilities-title"><div className="ps-utility-intro"><span className="ps-eyebrow">THAO TÁC NHANH</span><h2 id="ps-utilities-title">Bạn đang cần gì?</h2><p>Chọn một tiện ích để xem trạng thái dịch vụ và hướng tiếp theo.</p></div><div className="ps-utility-box"><div className="ps-utility-tabs" role="tablist" aria-label="Tiện ích công khai">{utilities.map(item => <button key={item.id} type="button" role="tab" id={`tab-${item.id}`} aria-selected={selected === item.id} aria-controls="ps-utility-panel" className={selected === item.id ? 'is-selected' : ''} onClick={() => setSelected(item.id)}><Glyph name={item.icon} /><span>{item.title}</span></button>)}</div><div className="ps-utility-panel" id="ps-utility-panel" role="tabpanel" aria-labelledby={`tab-${selected}`}><div><span className="ps-status">{selected === 'faq' ? 'THÔNG TIN SẴN CÓ' : 'SẮP RA MẮT'}</span><h3>{utility.title}</h3><p>{utility.description}</p><Link className="ps-text-link" to={utility.path}>{selected === 'faq' ? 'Xem câu hỏi thường gặp' : 'Xem thông tin tiện ích'} <Glyph name="arrow" /></Link></div><div className="ps-utility-visual" aria-hidden="true"><Glyph name={utility.icon} /><span className="ps-utility-orbit one" /><span className="ps-utility-orbit two" /></div></div></div></section>
    <section className="ps-section ps-services" id="services"><div className="ps-container"><SectionHeading eyebrow="HÀNH TRÌNH CHUYỂN PHÁT" title="Một nền tảng cho nhiều nhu cầu gửi hàng" description="Các trải nghiệm được phát triển theo từng luồng nghiệp vụ và chỉ mở khi có đầy đủ điều kiện vận hành." /><div className="ps-service-grid"><article className="ps-service-card"><span className="ps-service-icon"><Glyph name="box" /></span><span className="ps-service-number">01 / CÁ NHÂN</span><h3>Gửi hàng cá nhân</h3><p>Tạo tài khoản, quản lý hồ sơ và chuẩn bị cho các chức năng gửi hàng trong tương lai.</p><Link to="/services">Tìm hiểu dịch vụ <Glyph name="arrow" /></Link></article><article className="ps-service-card"><span className="ps-service-icon"><Glyph name="station" /></span><span className="ps-service-number">02 / DOANH NGHIỆP</span><h3>Không gian Merchant</h3><p>Hồ sơ Merchant bắt đầu từ tài khoản cá nhân đã xác minh; quyền tổ chức chỉ được cấp qua quy trình xét duyệt.</p><Link to="/services">Tìm hiểu quy trình <Glyph name="arrow" /></Link></article><article className="ps-service-card"><span className="ps-service-icon"><Glyph name="route" /></span><span className="ps-service-number">03 / VẬN HÀNH</span><h3>Mạng lưới giao nhận</h3><p>Station, Hub, tuyến vận chuyển và Courier là các mắt xích của cùng một hệ thống chuyển phát.</p><Link to="/about">Xem mô hình vận hành <Glyph name="arrow" /></Link></article></div></div></section>
    <section className="ps-section ps-process"><div className="ps-container ps-process-grid"><div><SectionHeading eyebrow="CÁCH E-LOGISTICS KẾT NỐI" title="Từ điểm gửi đến điểm nhận, cùng một dòng thông tin." /><p>Hành trình sản phẩm được thiết kế để mỗi bước có trách nhiệm rõ ràng. Các tính năng nghiệp vụ sẽ được mở khi hoàn tất kiểm chứng tương ứng.</p><Link className="ps-button ps-button-dark" to="/about">Khám phá E-Logistics <Glyph name="arrow" /></Link></div><ol className="ps-steps"><li><span>01</span><div><strong>Chuẩn bị</strong><p>Người gửi quản lý tài khoản và thông tin cần cho yêu cầu chuyển phát.</p></div></li><li><span>02</span><div><strong>Tiếp nhận & phân loại</strong><p>Station và Hub phối hợp xử lý kiện hàng theo trách nhiệm được giao.</p></div></li><li><span>03</span><div><strong>Vận chuyển & giao nhận</strong><p>Tuyến vận chuyển và Courier cập nhật các mốc của hành trình.</p></div></li><li><span>04</span><div><strong>Theo dõi & hỗ trợ</strong><p>Khách hàng tra cứu trạng thái và gửi yêu cầu hỗ trợ khi dịch vụ sẵn sàng.</p></div></li></ol></div></section>
    <section className="ps-section ps-updates"><div className="ps-container"><div className="ps-heading-row"><SectionHeading eyebrow="GÓC THÔNG TIN" title="Cập nhật & hướng dẫn" description="Nội dung giới thiệu sản phẩm, không phải tin tức hoặc thành tích doanh nghiệp đã công bố." /><Link className="ps-text-link" to="/news">Xem tất cả <Glyph name="arrow" /></Link></div><div className="ps-update-grid"><article className="ps-update-feature"><span className="ps-update-art"><Glyph name="route" /></span><div><span className="ps-tag">HƯỚNG DẪN</span><h3>Bắt đầu với tài khoản cá nhân</h3><p>Đăng ký bằng email, xác minh địa chỉ và đăng nhập vào không gian cá nhân.</p><Link to="/news">Đọc hướng dẫn <Glyph name="arrow" /></Link></div></article><div className="ps-update-stack"><article><span className="ps-tag">NỀN TẢNG</span><h3>Một tài khoản cá nhân trước khi đăng ký Merchant</h3><p>Hồ sơ doanh nghiệp và quyền truy cập tổ chức là hai bước tách biệt.</p><Link to="/news">Xem thêm <Glyph name="arrow" /></Link></article><article><span className="ps-tag">BẢO MẬT</span><h3>Xác minh email và bảo vệ phiên đăng nhập</h3><p>Liên kết xác minh và khôi phục được xử lý qua các luồng có kiểm soát.</p><Link to="/news">Xem thêm <Glyph name="arrow" /></Link></article></div></div></div></section>
    <section className="ps-end-cta"><div className="ps-container ps-end-cta-inner"><div><span className="ps-eyebrow">BẮT ĐẦU CÙNG E-LOGISTICS</span><h2>Không gian cá nhân của bạn đã sẵn sàng.</h2><p>Tạo tài khoản, xác minh email và khám phá các chức năng đang được phát triển.</p></div><Link className="ps-button ps-button-light" to="/register">Tạo tài khoản cá nhân <Glyph name="arrow" /></Link></div></section>
  </>;
}

const pages: Record<string, { eyebrow: string; title: string; description: string; body: string; coming?: boolean }> = {
  '/services': { eyebrow: 'DỊCH VỤ', title: 'Dịch vụ chuyển phát', description: 'Hành trình dành cho người gửi cá nhân và doanh nghiệp.', body: 'E-Logistics đang phát triển các luồng gửi hàng, lấy hàng, phân loại, vận chuyển và giao nhận trên cùng nền tảng. Bảng giá, vùng phục vụ và thời gian cam kết sẽ được công bố sau khi cấu hình vận hành được xác nhận.', coming: true },
  '/tracking': { eyebrow: 'TIỆN ÍCH', title: 'Tra cứu vận đơn', description: 'Tính năng tra cứu công khai đang được phát triển.', body: 'Chưa có API tra cứu vận đơn công khai được nghiệm thu. Trang này không tạo trạng thái vận đơn hoặc kết quả tra cứu minh họa.', coming: true },
  '/quote': { eyebrow: 'TIỆN ÍCH', title: 'Ước tính cước phí', description: 'Báo giá chỉ có giá trị khi chính sách giá được xác nhận.', body: 'Công cụ ước tính cước và bảng giá chưa được công bố. E-Logistics sẽ hiển thị kết quả khi có chính sách dịch vụ và dữ liệu hợp lệ.', coming: true },
  '/coverage': { eyebrow: 'TIỆN ÍCH', title: 'Vùng phục vụ', description: 'Danh mục địa danh đang được chuẩn bị.', body: 'Thông tin vùng phục vụ cần được đối chiếu với cấu hình Station, Hub và tuyến vận hành trước khi cung cấp kết quả tra cứu.', coming: true },
  '/stations': { eyebrow: 'MẠNG LƯỚI', title: 'Tìm bưu cục', description: 'Các điểm Station và Hub sẽ được hiển thị khi đã có dữ liệu chính thức.', body: 'E-Logistics chưa công bố địa chỉ bưu cục. Không sử dụng địa điểm minh họa để hướng dẫn gửi hàng thực tế.', coming: true },
  '/about': { eyebrow: 'VỀ E-LOGISTICS', title: 'Một mạng lưới chuyển phát có kết nối', description: 'Nền tảng quản lý chuyển phát cho người gửi, Merchant và đội vận hành.', body: 'Mô hình E-Logistics kết nối pickup, Station, Hub, phân loại, tuyến vận chuyển và giao nhận. Mỗi chặng có quyền thao tác và dữ liệu riêng, cùng đóng góp vào một hành trình mà khách hàng có thể theo dõi khi các dịch vụ được triển khai.' },
  '/support': { eyebrow: 'HỖ TRỢ', title: 'Hỗ trợ khách hàng', description: 'Tìm câu trả lời và theo dõi các kênh hỗ trợ chính thức.', body: 'Câu hỏi về tài khoản có thể xem trong mục FAQ. Kênh liên hệ, số điện thoại và địa chỉ hỗ trợ chính thức đang được Owner cung cấp; website chưa công bố thông tin thay thế.' },
  '/privacy': { eyebrow: 'CHÍNH SÁCH', title: 'Bảo mật dữ liệu', description: 'Nội dung chính sách chính thức đang được chuẩn bị.', body: 'Trang này là vị trí dành cho chính sách bảo vệ dữ liệu sau khi được phê duyệt. Chưa sử dụng nội dung này làm văn bản pháp lý hoặc bằng chứng đồng ý chính sách.', coming: true },
  '/terms': { eyebrow: 'CHÍNH SÁCH', title: 'Điều khoản sử dụng', description: 'Điều khoản chính thức đang được chuẩn bị.', body: 'Các điều kiện dịch vụ và điều khoản sử dụng sẽ được công bố sau khi Owner phê duyệt nội dung. Hiện chưa có cam kết thương mại từ trang này.', coming: true },
  '/shipping-policy': { eyebrow: 'CHÍNH SÁCH', title: 'Chính sách vận chuyển', description: 'Chính sách vận chuyển đang được hoàn thiện.', body: 'Thông tin về vùng phục vụ, thời gian, phí, hàng hạn chế và xử lý sự cố sẽ được công bố cùng dịch vụ vận chuyển chính thức.', coming: true },
};

const guides = [
  { category: 'Hướng dẫn', title: 'Tạo và xác minh tài khoản cá nhân', text: 'Bắt đầu bằng email của bạn. Sau khi xác minh, bạn có thể đăng nhập và quản lý hồ sơ cá nhân.' },
  { category: 'Nền tảng', title: 'Đăng ký trở thành Merchant diễn ra thế nào?', text: 'Tài khoản cá nhân có thể chuẩn bị bản nháp; quyền tổ chức chỉ có sau bước xét duyệt hợp lệ.' },
  { category: 'Hướng dẫn', title: 'Khôi phục quyền truy cập tài khoản', text: 'Yêu cầu hướng dẫn qua email đã xác minh và đặt lại mật khẩu bằng liên kết còn hiệu lực.' },
];

function News() {
  const [filter, setFilter] = useState('Tất cả');
  return <section className="ps-inner-page ps-container"><span className="ps-eyebrow">GÓC THÔNG TIN</span><h1>Cập nhật & hướng dẫn</h1><p className="ps-inner-lead">Tài liệu giới thiệu trải nghiệm E-Logistics đang phát triển. Không có tin tức doanh nghiệp hay thành tích giả.</p><div className="ps-filters" aria-label="Lọc nội dung">{['Tất cả', 'Hướng dẫn', 'Nền tảng'].map(item => <button key={item} type="button" aria-pressed={filter === item} onClick={() => setFilter(item)}>{item}</button>)}</div><div className="ps-guide-grid">{guides.filter(item => filter === 'Tất cả' || filter === item.category).map(item => <article key={item.title}><span className="ps-tag">{item.category.toUpperCase()}</span><h2>{item.title}</h2><p>{item.text}</p></article>)}</div></section>;
}

function Faq() {
  return <section className="ps-inner-page ps-container"><span className="ps-eyebrow">HỖ TRỢ</span><h1>Câu hỏi thường gặp</h1><p className="ps-inner-lead">Những bước tài khoản hiện đã có trên hệ thống.</p><div className="ps-faq-list"><details><summary>Tôi cần gì để tạo tài khoản?</summary><p>Bạn cần họ tên hiển thị, địa chỉ email, số điện thoại có mã quốc gia và mật khẩu hợp lệ. Email phải được xác minh trước khi đăng nhập; số điện thoại hiện chỉ là liên hệ chưa xác minh.</p></details><details><summary>Quên mật khẩu thì làm thế nào?</summary><p>Vào trang <Link to="/recover">khôi phục mật khẩu</Link>, nhập email đã xác minh và làm theo hướng dẫn trong thư. Hệ thống không tiết lộ địa chỉ nào đã đăng ký.</p></details><details><summary>Tạo tài khoản có tự cấp quyền Merchant không?</summary><p>Không. Tài khoản cá nhân có thể chuẩn bị hồ sơ Merchant; quyền tổ chức cần quy trình xét duyệt và cấp quyền riêng.</p></details><details><summary>Tôi đã có thể tra cứu vận đơn và xem giá chưa?</summary><p>Hai tiện ích này đang được phát triển và hiện chưa trả kết quả chính thức.</p></details></div></section>;
}

function InfoPage({ path }: { path: string }) {
  const page = pages[path];
  if (!page) return <section className="ps-inner-page ps-container"><span className="ps-eyebrow">KHÔNG TÌM THẤY</span><h1>Trang bạn tìm chưa có</h1><p className="ps-inner-lead">Đường dẫn này không thuộc website hiện tại.</p><Link className="ps-button ps-button-dark" to="/">Về trang chủ</Link></section>;
  return <section className="ps-inner-page ps-container"><span className="ps-eyebrow">{page.eyebrow}</span><h1>{page.title}</h1><p className="ps-inner-lead">{page.description}</p><div className="ps-info-grid"><article><span className="ps-status">{page.coming ? 'ĐANG PHÁT TRIỂN' : 'GIỚI THIỆU'}</span><p>{page.body}</p><div className="ps-info-actions"><Link className="ps-button ps-button-dark" to="/">Về trang chủ <Glyph name="arrow" /></Link><Link className="ps-text-link" to="/faq">Xem FAQ</Link></div></article><aside><Glyph name={path === '/stations' ? 'station' : path === '/coverage' ? 'pin' : 'route'} /><strong>Rõ thông tin ở từng bước</strong><p>Chúng tôi chỉ hiển thị dữ liệu dịch vụ khi nguồn và điều kiện vận hành đã được xác nhận.</p></aside></div></section>;
}

export default function PublicWebsite() {
  const { pathname } = useLocation();
  return <div className="public-site"><a className="ps-skip" href="#public-main">Đến nội dung chính</a><div className="ps-topline"><div className="ps-container">E-LOGISTICS <span>·</span> Nền tảng chuyển phát đang được phát triển</div></div><PublicHeader /><main id="public-main">{pathname === '/' ? <Home /> : pathname === '/news' ? <News /> : pathname === '/faq' ? <Faq /> : <InfoPage path={pathname} />}</main><PublicFooter /></div>;
}
