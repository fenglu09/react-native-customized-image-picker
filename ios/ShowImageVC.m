//
//  ShowImageVC.m
//  imageCropPicker
//
//  Created by 欧阳伟坚 on 2018/4/3.
//  Copyright © 2018年 Ivan Pusic. All rights reserved.
//

#import "ShowImageVC.h"

#define getRectNavAndStatusHight  self.navigationController.navigationBar.frame.size.height+[[UIApplication sharedApplication] statusBarFrame].size.height

#define iPhone6sScreenWidth 375.0
#define iPhone6sScreenHeight 667.0
#define KScreenWidth [UIScreen mainScreen].bounds.size.width
#define KScreenHeight [UIScreen mainScreen].bounds.size.height
//功能页面按钮的自动布局
#define AutoLayoutFunctionBtnSizeX(X) KScreenWidth*(X)/iPhone6sScreenWidth
#define AutoLayoutFunctionBtnSizeY(Y) KScreenHeight*(Y)/iPhone6sScreenHeight
#define AutoLayoutFunctionBtnWidth(width) KScreenWidth*(width)/iPhone6sScreenWidth
#define AutoLayoutFunctionBtnHeight(height) KScreenHeight*(height)/iPhone6sScreenHeight

@interface ShowImageVC ()

@end

@implementation ShowImageVC

- (void)viewDidLoad {
    [super viewDidLoad];
    
    // 隐藏电源状态栏
    [self setNeedsStatusBarAppearanceUpdate];
    [self prefersStatusBarHidden];
    
    self.view.backgroundColor = [UIColor blackColor];
    
    UIImage *newImage = [self imageAddText:self.dataImage text:self.location name:self.name];
    
    UIImageView *imageView = [[UIImageView alloc]initWithImage:newImage];
    
    
    
//    imageView.frame = CGRectMake(0, 0, KScreenWidth, KScreenHeight);
    if (self.isAcross) {
        imageView.frame = imageView.frame = CGRectMake(0, 0, KScreenWidth, AutoLayoutFunctionBtnHeight(210));
        imageView.center = CGPointMake(KScreenWidth / 2, KScreenHeight / 2);
    }
    else{
      imageView.frame = CGRectMake(0, KScreenHeight * 0.05, KScreenWidth, KScreenHeight * 0.85);
    }
    imageView.contentMode = UIViewContentModeScaleToFill;
    [self.view addSubview:imageView];
    
    UIButton *leftButton = [UIButton buttonWithType:UIButtonTypeCustom];
    [leftButton setTitle:@"重拍" forState:UIControlStateNormal];
    leftButton.titleLabel.textAlignment = NSTextAlignmentCenter;
    [leftButton sizeToFit];
    leftButton.center = CGPointMake(30, KScreenHeight-30);
    [leftButton addTarget:self action:@selector(popView) forControlEvents:UIControlEventTouchUpInside];
    [self.view addSubview:leftButton];
    
    UIButton *rightBtn = [UIButton buttonWithType:UIButtonTypeCustom];
    [rightBtn setTitle:@"使用照片" forState:UIControlStateNormal];
    rightBtn.titleLabel.textAlignment = NSTextAlignmentCenter;
    [rightBtn sizeToFit];
    rightBtn.center = CGPointMake(KScreenWidth - 50, KScreenHeight-30);
    [rightBtn addTarget:self action:@selector(makeSureImage) forControlEvents:UIControlEventTouchUpInside];
    [self.view addSubview:rightBtn];
    self.dataImage = newImage;
}

//隐藏单个页面电池条的方法
- (BOOL)prefersStatusBarHidden{
    return YES;  //隐藏
}

-(void) popView {
    self.dataImage = nil;
    
    [self dismissViewControllerAnimated:YES completion:nil];
}

-(void) makeSureImage {
    
    NSDictionary *dic = [NSDictionary dictionaryWithObjectsAndKeys:self.dataImage,@"image", nil];
    
    NSNotification *notification = [NSNotification notificationWithName:@"image" object:dic];
    
    [[NSNotificationCenter defaultCenter] postNotification:notification];
}

// 给图片添加文字水印：
- (UIImage *)imageAddText:(UIImage *)img text:(NSString *)logoText name:(NSString *)name{
    
    NSString* mark = logoText;
    
    int w = img.size.width;
    int h = img.size.height;
    
    UIGraphicsBeginImageContext(img.size);
    [img drawInRect:CGRectMake(0, 0, w, h)];
    
    // 图片icon
    UIImage *locationIcon = [UIImage imageNamed:@"location"];
    UIImage *contactIcon = [UIImage imageNamed:@"contact"];
    
    // 文字样式
    NSDictionary *hourStyle = @{NSFontAttributeName: [UIFont systemFontOfSize:45], NSForegroundColorAttributeName: [UIColor whiteColor]};
    NSDictionary *dayStyle = @{NSFontAttributeName: [UIFont systemFontOfSize:25], NSForegroundColorAttributeName: [UIColor whiteColor]};
    NSDictionary *weekStyle = @{NSFontAttributeName: [UIFont boldSystemFontOfSize:25], NSForegroundColorAttributeName: [UIColor whiteColor]};
    NSDictionary *lineStyle = @{NSBackgroundColorAttributeName: [UIColor whiteColor]};
    
    //获取当前时间
    NSDateFormatter *formatter = [[NSDateFormatter alloc] init];
    [formatter setDateFormat:@"YYYY.MM.dd"];
    NSDateFormatter *timeFormatter = [[NSDateFormatter alloc]init];
    [timeFormatter setDateFormat:@"HH:mm"];
    NSDate *dateNow = [NSDate date];
    NSString *weekStr = [self weekdayStringFromDate:dateNow];
    NSString *currentTimeString = [formatter stringFromDate:dateNow];
    NSString *timeStr = [timeFormatter stringFromDate:dateNow];
    NSString *lineStr = @"\n \n \n \n \n \n \n \n \n \n \n";
    
    CGFloat xDistance = 30.0;
    
    //位置显示
    [lineStr drawInRect:CGRectMake(AutoLayoutFunctionBtnSizeX(xDistance), AutoLayoutFunctionBtnSizeY(30), AutoLayoutFunctionBtnWidth(5), AutoLayoutFunctionBtnHeight(120)) withAttributes:lineStyle];
    
    [timeStr drawInRect:CGRectMake(AutoLayoutFunctionBtnSizeX(xDistance + 15), AutoLayoutFunctionBtnSizeY(25), AutoLayoutFunctionBtnWidth(120), AutoLayoutFunctionBtnHeight(45)) withAttributes:hourStyle];
    [currentTimeString drawInRect:CGRectMake(AutoLayoutFunctionBtnSizeX(xDistance + 140), AutoLayoutFunctionBtnSizeY(42), AutoLayoutFunctionBtnWidth(120), AutoLayoutFunctionBtnHeight(40)) withAttributes:dayStyle];
    [weekStr drawInRect:CGRectMake(AutoLayoutFunctionBtnSizeX(xDistance + 260), AutoLayoutFunctionBtnSizeY(41), AutoLayoutFunctionBtnWidth(80), AutoLayoutFunctionBtnHeight(40)) withAttributes:weekStyle];
    
    [locationIcon drawInRect:CGRectMake(AutoLayoutFunctionBtnSizeX(xDistance + 15), AutoLayoutFunctionBtnSizeY(80), AutoLayoutFunctionBtnWidth(25), AutoLayoutFunctionBtnHeight(25))];
    
    [mark drawInRect:CGRectMake(AutoLayoutFunctionBtnSizeX(xDistance + 50), AutoLayoutFunctionBtnSizeY(80), w, AutoLayoutFunctionBtnHeight(40)) withAttributes:dayStyle];
    
    [contactIcon drawInRect:CGRectMake(AutoLayoutFunctionBtnSizeX(xDistance + 15), AutoLayoutFunctionBtnSizeY(120), AutoLayoutFunctionBtnWidth(25), AutoLayoutFunctionBtnHeight(25))];
    
    [name drawInRect:CGRectMake(AutoLayoutFunctionBtnSizeX(xDistance + 50), AutoLayoutFunctionBtnSizeY(120), w, AutoLayoutFunctionBtnHeight(40)) withAttributes:dayStyle];
    
    UIImage *aimg = UIGraphicsGetImageFromCurrentImageContext();
    UIGraphicsEndImageContext();
    
    return aimg;
}

- (NSString*)weekdayStringFromDate:(NSDate*)inputDate {
    
    NSArray *weekdays = [NSArray arrayWithObjects: [NSNull null], @"星期天", @"星期一", @"星期二", @"星期三", @"星期四", @"星期五", @"星期六", nil];
    
    NSCalendar *calendar = [[NSCalendar alloc] initWithCalendarIdentifier:NSCalendarIdentifierGregorian];
    
    NSTimeZone *timeZone = [[NSTimeZone alloc] initWithName:@"Asia/Shanghai"];
    
    [calendar setTimeZone: timeZone];
    
    NSCalendarUnit calendarUnit = NSCalendarUnitWeekday;
    
    NSDateComponents *theComponents = [calendar components:calendarUnit fromDate:inputDate];
    
    return [weekdays objectAtIndex:theComponents.weekday];
    
}



@end
