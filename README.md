# do_BarcodeView
条码扫描视图，能设置view的基本属性，宽高等，扫描框内有矩形校正区域，扫描时有简单的动画显示，支持一维码、二维码（包括QR码、DM码等），若想在页面已启动时就加载扫描功能，建议将start方法放在do_Page的loaded事件回调中执行
