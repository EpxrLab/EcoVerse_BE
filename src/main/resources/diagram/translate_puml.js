const fs = require('fs');
const path = require('path');

const dir = 'd:\\school\\SEP490\\fixBe\\EcoVerse_BE\\src\\main\\resources\\diagram';
const files = fs.readdirSync(dir).filter(f => f.startsWith('seq_') && f.endsWith('.puml'));

const dictionary = [
    [/Học sinh\/Phụ huynh/gi, "Student/Parent"],
    [/Nhà trường\/Đối tác/gi, "School/Partnership"],
    [/Học sinh/gi, "Student"],
    [/Phụ huynh/gi, "Parent"],
    [/Quản trị viên/gi, "Admin"],
    [/Nhà trường/gi, "School"],
    [/Đối tác/gi, "Partnership"],
    [/Trường học/gi, "School"],
    [/Người dùng/gi, "User"],
    [/Sinh viên/gi, "Student"],
    [/Ngân hàng/gi, "Bank"],
    [/Khởi tạo/gi, "Initialize"],
    [/Tạo mới/gi, "Create new"],
    [/Tạo/gi, "Create"],
    [/Gói đăng ký/gi, "Subscription plan"],
    [/Gói chờ/gi, "Pending plan"],
    [/Gói/gi, "Plan"],
    [/Quản lý/gi, "Management"],
    [/Yêu cầu đổi quà/gi, "Reward request"],
    [/Yêu cầu/gi, "Request"],
    [/yêu cầu/gi, "request"],
    [/Quà thưởng/gi, "Reward"],
    [/quà/gi, "reward"],
    [/Chi tiết/gi, "Details"],
    [/Danh sách/gi, "List of"],
    [/Lịch sử/gi, "History"],
    [/Bảng/gi, "Table"],
    [/trạng thái/gi, "status"],
    [/Hoàn thành/gi, "Complete"],
    [/Kết quả/gi, "Result"],
    [/Báo lỗi/gi, "Report error"],
    [/Thông báo lỗi/gi, "error message"],
    [/Hiển thị lỗi/gi, "Display error"],
    [/Hiển thị dialog xác nhận/gi, "Display confirmation dialog"],
    [/Thông báo/gi, "notification"],
    [/Hiển thị thông báo/gi, "Display message"],
    [/Hiển thị/gi, "Display"],
    [/Mở trang/gi, "Open page"],
    [/Vào trang/gi, "Navigate to page"],
    [/Trang/gi, "Page"],
    [/Nhấn vào mục/gi, "Click item"],
    [/Nhấn vào/gi, "Click on"],
    [/Nhấn nút/gi, "Click button"],
    [/Nhấn/gi, "Click"],
    [/Chọn/gi, "Select"],
    [/chọn/gi, "select"],
    [/Nhập lại/gi, "Re-enter"],
    [/đăng nhập/gi, "login"],
    [/Đăng ký/gi, "Register"],
    [/Đăng nhập/gi, "Login"],
    [/Nhập/gi, "Enter"],
    [/nhập/gi, "enter"],
    [/Xem/gi, "View"],
    [/Gửi/gi, "Send"],
    [/Kiểm tra/gi, "Check"],
    [/Trả về/gi, "Return"],
    [/Xác nhận nhận/gi, "Confirm receiving"],
    [/Xác nhận/gi, "Confirm"],
    [/Thành công/gi, "Successfully"],
    [/thành công/gi, "successfully"],
    [/hoàn coin/gi, "refund coins"],
    [/hoàn stock/gi, "refund stock"],
    [/tồn kho/gi, "inventory stock"],
    [/hết hàng/gi, "out of stock"],
    [/giữ chỗ/gi, "reserve"],
    [/trừ coin ngay/gi, "deduct coins immediately"],
    [/Lý do/gi, "Reason"],
    [/lý do/gi, "reason"],
    [/tùy chọn/gi, "optional"],
    [/Không tìm thấy/gi, "Not found"],
    [/đã tồn tại/gi, "already exists"],
    [/đã được/gi, "has been"],
    [/đã/gi, "has"],
    [/chưa được xác thực/gi, "not yet verified"],
    [/chưa được/gi, "not yet"],
    [/chưa/gi, "not yet"],
    [/trên form/gi, "on form"],
    [/phía client/gi, "client side"],
    [/bắt buộc/gi, "required"],
    [/rỗng/gi, "empty"],
    [/không hợp lệ/gi, "invalid"],
    [/hợp lệ/gi, "valid"],
    [/có thể/gi, "can"],
    [/Hủy/gi, "Cancel"],
    [/Từ chối/gi, "Reject"],
    [/Duyệt/gi, "Approve"],
    [/Cập nhật/gi, "Update"],
    [/Xóa mềm/gi, "Soft delete"],
    [/Xóa/gi, "Delete"],
    [/với nút/gi, "with button"],
    [/cho mỗi item/gi, "for each item"],
    [/Tất cả/gi, "All"],
    [/tất cả/gi, "all"],
    [/bị xóa theo/gi, "will be deleted"],
    [/giao/gi, "deliver"],
    [/nhận/gi, "receive"],
    [/Gia hạn/gi, "Renew"],
    [/Kích hoạt/gi, "Activate"],
    [/đang hoạt động/gi, "active"],
    [/đang sử dụng/gi, "in use"],
    [/đang chờ Admin/gi, "waiting for Admin"],
    [/đang chờ/gi, "pending"],
    [/Điền thông tin và/gi, "Fill details and"],
    [/Điền thông tin/gi, "Fill details"],
    [/Lưu/gi, "Save"],
    [/Thêm/gi, "Add"],
    [/Nếu/gi, "If"],
    [/thêm/gi, "add"],
    [/trường/gi, "field"],
    [/Mã xác nhận/gi, "OTP code"],
    [/hệ thống/gi, "system"],
    [/không được trùng nhau/gi, "must not duplicate"],
    [/phải có ít nhất/gi, "must have at least"],
    [/trùng/gi, "duplicate"],
    [/phiên bản/gi, "version"],
    [/phiên chơi/gi, "game session"],
    [/phiên/gi, "session"],
    [/trước đó/gi, "previous"],
    [/trung tâm/gi, "center"],
    [/chờ hệ thống xét duyệt/gi, "waiting for system approval"],
    [/Tài khoản/gi, "Account"],
    [/Chuyển đến/gi, "Navigate to"],
    [/Chuyển sang form/gi, "Switch to form"],
    [/hoặc đã hết hạn/gi, "or has expired"],
    [/hết hạn/gi, "expired"],
    [/trống/gi, "empty"],
    [/Chỉnh sửa/gi, "Edit"],
    [/dữ liệu/gi, "data"],
    [/được fill sẵn/gi, "pre-filled"],
    [/sẵn/gi, "ready"],
    [/thay đổi/gi, "change"],
    [/Sửa/gi, "Edit"],
    [/sửa/gi, "edit"],
    [/cần/gi, "needs to"],
    [/bị/gi, "be"],
    [/vào một/gi, "into a"],
    [/trong/gi, "in"],
    [/của/gi, "of"],
    
    [/(?<=^|\\s|>|:|-|\[|\(|\|)\s*cho\s*(?=$|\s|<|:|\]|\)|,|\|)/g, " for "],
    [/(?<=^|\\s|>|:|-|\[|\(|\|)\s*từ\s*(?=$|\s|<|:|\]|\)|,|\|)/g, " from "],
    [/(?<=^|\\s|>|:|-|\[|\(|\|)\s*để\s*(?=$|\s|<|:|\]|\)|,|\|)/g, " to "],
    [/(?<=^|\\s|>|:|-|\[|\(|\|)\s*với\s*(?=$|\s|<|:|\]|\)|,|\|)/g, " with "],
    [/(?<=^|\\s|>|:|-|\[|\(|\|)\s*các\s*(?=$|\s|<|:|\]|\)|,|\|)/g, " the "],
    [/(?<=^|\\s|>|:|-|\[|\(|\|)\s*mới\s*(?=$|\s|<|:|\]|\)|,|\|)/g, " new "],
    [/(?<=^|\\s|>|:|-|\[|\(|\|)\s*hiện có\s*(?=$|\s|<|:|\]|\)|,|\|)/g, " existing "],
    [/(?<=^|\\s|>|:|-|\[|\(|\|)\s*hoặc\s*(?=$|\s|<|:|\]|\)|,|\|)/g, " or "],
    [/(?<=^|\\s|>|:|-|\[|\(|\|)\s*và\s*(?=$|\s|<|:|\]|\)|,|\|)/g, " and "],
    [/(?<=^|\\s|>|:|-|\[|\(|\|)\s*có\s*(?=$|\s|<|:|\]|\)|,|\|)/g, " has "],
    [/(?<=^|\\s|>|:|-|\[|\(|\|)\s*không\s*(?=$|\s|<|:|\]|\)|,|\|)/g, " not "],
    [/(?<=^|\\s|>|:|-|\[|\(|\|)\s*đúng\s*(?=$|\s|<|:|\]|\)|,|\|)/g, " correct "],
    [/(?<=^|\\s|>|:|-|\[|\(|\|)\s*sai\s*(?=$|\s|<|:|\]|\)|,|\|)/g, " incorrect "],
    [/(?<=^|\\s|>|:|-|\[|\(|\|)\s*thiếu\s*(?=$|\s|<|:|\]|\)|,|\|)/g, " missing "],
    [/(?<=^|\\s|>|:|-|\[|\(|\|)\s*bằng\s*(?=$|\s|<|:|\]|\)|,|\|)/g, " equals "],
    [/(?<=^|\\s|>|:|-|\[|\(|\|)\s*Mở\s*(?=$|\s|<|:|\]|\)|,|\|)/g, " Open "],
    [/(?<=^|\\s|>|:|-|\[|\(|\|)\s*thỏa mãn\s*(?=$|\s|<|:|\]|\)|,|\|)/g, " matches "],
    [/(?<=^|\\s|>|:|-|\[|\(|\|)\s*điều kiện\s*(?=$|\s|<|:|\]|\)|,|\|)/g, " condition "],
    [/(?<=^|\\s|>|:|-|\[|\(|\|)\s*sẽ\s*(?=$|\s|<|:|\]|\)|,|\|)/g, " will "],
    [/(?<=^|\\s|>|:|-|\[|\(|\|)\s*khác\s*(?=$|\s|<|:|\]|\)|,|\|)/g, " other "],
    [/(?<=^|\\s|>|:|-|\[|\(|\|)\s*nữa\s*(?=$|\s|<|:|\]|\)|,|\|)/g, " anymore "],
    [/(?<=^|\\s|>|:|-|\[|\(|\|)\s*này\s*(?=$|\s|<|:|\]|\)|,|\|)/g, " this "],
    [/(?<=^|\\s|>|:|-|\[|\(|\|)\s*khi\s*(?=$|\s|<|:|\]|\)|,|\|)/g, " when "],
    [/(?<=^|\\s|>|:|-|\[|\(|\|)\s*hiện tại\s*(?=$|\s|<|:|\]|\)|,|\|)/g, " current "]
];

function translate(text) {
    let result = text;
    // Hide quoted strings
    const quotes = [];
    result = result.replace(/"([^"\\]|\\.)*"/g, (match) => {
        quotes.push(match);
        return `__QUOTE_${quotes.length - 1}__`;
    });
    
    // Apply dictionary logic
    for (let [pattern, replacement] of dictionary) {
        result = result.replace(pattern, replacement);
    }
    
    // Cleanup multiple spaces
    result = result.replace(/ +/g, ' ').replace(/ \n/g, '\n');

    // Capitalize first letter of message notes
    result = result.replace(/^([ \t]*[a-zA-Z0-9_\-]+[ \t]*->[ \t]*[a-zA-Z0-9_\-]+[ \t]*:[ \t]*)(.*)$/gm, (match, prefix, content) => {
        if (content.trim().length > 0) {
            return prefix + content.charAt(0).toUpperCase() + content.slice(1);
        }
        return match;
    });

    // Restore quoted strings
    result = result.replace(/__QUOTE_(\d+)__/g, (match, index) => {
        return quotes[parseInt(index, 10)];
    });
    
    return result;
}

let modifiedCount = 0;
files.forEach(f => {
    let content = fs.readFileSync(path.join(dir, f), 'utf-8');
    let orig = content;
    content = translate(content);
    if (content !== orig) {
        fs.writeFileSync(path.join(dir, f), content, 'utf-8');
        console.log('Translated: ' + f);
        modifiedCount++;
    }
});
console.log('Total files translated: ' + modifiedCount);
