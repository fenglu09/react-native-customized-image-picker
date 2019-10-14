//
//  MuguCameraVC.m
//  imageCropPicker
//
//  Created by 欧阳伟坚 on 2018/4/3.
//  Copyright © 2018年 Ivan Pusic. All rights reserved.
//

#import "MuguCameraVC.h"

#define iPhone6pScreenWidth 414.0
#define iPhone6pScreenHeight 736.0
#define KScreenWidth  [UIScreen mainScreen].bounds.size.width
#define KScreenHeight  [UIScreen mainScreen].bounds.size.height
//功能页面按钮的自动布局
#define AutoLayoutFunctionBtnSizeX(X) KScreenWidth*(X)/iPhone6pScreenWidth
#define AutoLayoutFunctionBtnSizeY(Y) KScreenHeight*(Y)/iPhone6pScreenHeight
#define AutoLayoutFunctionBtnWidth(width) KScreenWidth*(width)/iPhone6pScreenWidth
#define AutoLayoutFunctionBtnHeight(height) KScreenHeight*(height)/iPhone6pScreenHeight

//导入相机框架
#import <AVFoundation/AVFoundation.h>
//将拍摄好的照片写入系统相册中，所以我们在这里还需要导入一个相册需要的头文件iOS8
#import <Photos/Photos.h>
#import "ShowImageVC.h"
#import "UIImage+DJResize.h"
#import "Orientation.h"
#import "DeviceOrientation.h"


@interface MuguCameraVC ()<UIAlertViewDelegate,DeviceOrientationDelegate>

//捕获设备，通常是前置摄像头，后置摄像头，麦克风（音频输入）
@property(nonatomic)AVCaptureDevice *device;

//AVCaptureDeviceInput 代表输入设备，他使用AVCaptureDevice 来初始化
@property(nonatomic)AVCaptureDeviceInput *input;

//当启动摄像头开始捕获输入
@property(nonatomic)AVCaptureMetadataOutput *output;

//照片输出流
@property (nonatomic)AVCaptureStillImageOutput *ImageOutPut;

//session：由他把输入输出结合在一起，并开始启动捕获设备（摄像头）
@property(nonatomic)AVCaptureSession *session;

//图像预览层，实时显示捕获的图像
@property(nonatomic)AVCaptureVideoPreviewLayer *previewLayer;

@property (strong, nonatomic) DeviceOrientation *deviceMotion;

@property (nonatomic)NSString *directionStr;

// ------------- UI --------------
//拍照按钮
@property (nonatomic)UIButton *photoButton;
//闪光灯按钮
@property (nonatomic)UIButton *flashButton;
//聚焦
@property (nonatomic)UIView *focusView;
//是否开启闪光灯
@property (nonatomic)BOOL isflashOn;

@property (nonatomic)int flag;
@end

@implementation MuguCameraVC

- (void)viewDidLoad {
    [super viewDidLoad];
    self.view.backgroundColor = [UIColor blackColor];
    
    self.deviceMotion = [[DeviceOrientation alloc]initWithDelegate:self];
    
    if ( [self checkCameraPermission]) {
        
        [self customCamera];
        [self initSubViews];
        
        [self focusAtPoint:CGPointMake(0.5, 0.5)];
        
        // 隐藏电源状态栏
        [self setNeedsStatusBarAppearanceUpdate];
        [self prefersStatusBarHidden];
        
        self.flag = 0;
        
        // RN禁止原生组件横屏效果
        [Orientation setOrientation:UIInterfaceOrientationMaskPortrait];
        
        
        [self.deviceMotion startMonitor];
    }
}

- (void)viewWillAppear:(BOOL)animated{
    
    [super viewWillAppear:YES];
    
    if (self.session) {
        
        [self.session startRunning];
    }
}

- (void)viewDidDisappear:(BOOL)animated{
    
    [super viewDidDisappear:YES];
    
    if (self.session) {
        
        [self.session stopRunning];
    }
}

//隐藏单个页面电池条的方法

- (BOOL)prefersStatusBarHidden{
    return YES;  //隐藏
}

- (void)directionChange:(TgDirection)direction {
    
    switch (direction) {
        case TgDirectionPortrait:
            self.directionStr = @"protrait";
            break;
        case TgDirectionDown:
            self.directionStr = @"down";
            break;
        case TgDirectionRight:
            self.directionStr = @"right";
            break;
        case TgDirectionleft:
            self.directionStr = @"left";
            break;
        default:
            break;
    }
}

- (UIViewController*) getRootVC {
    UIViewController *root = [[[[UIApplication sharedApplication] delegate] window] rootViewController];
    while (root.presentedViewController != nil) {
        root = root.presentedViewController;
    }
    
    return root;
}

- (void)customCamera
{
    //使用AVMediaTypeVideo 指明self.device代表视频，默认使用后置摄像头进行初始化
    self.device = [AVCaptureDevice defaultDeviceWithMediaType:AVMediaTypeVideo];
    //使用设备初始化输入
    self.input = [[AVCaptureDeviceInput alloc]initWithDevice:self.device error:nil];
    //生成输出对象
    self.output = [[AVCaptureMetadataOutput alloc]init];
    
    self.ImageOutPut = [[AVCaptureStillImageOutput alloc]init];
    //生成会话，用来结合输入输出
    self.session = [[AVCaptureSession alloc]init];
    // 2 设置session显示分辨率
    if ([[UIDevice currentDevice] userInterfaceIdiom] == UIUserInterfaceIdiomPhone){
        [self.session setSessionPreset:AVCaptureSessionPreset1280x720];
    }
    else {
        [self.session setSessionPreset:AVCaptureSessionPresetPhoto];
    }

    
    if ([self.session canAddInput:self.input]) {
        [self.session addInput:self.input];
        
    }
    
    if ([self.session canAddOutput:self.ImageOutPut]) {
        [self.session addOutput:self.ImageOutPut];
    }
    
    
    //使用self.session，初始化预览层，self.session负责驱动input进行信息的采集，layer负责把图像渲染显示
    self.previewLayer = [[AVCaptureVideoPreviewLayer alloc]initWithSession:self.session];
    //    self.previewLayer.frame = CGRectMake(0, 0, KScreenWidth, KScreenHeight);
    self.previewLayer.frame = CGRectMake(0, KScreenHeight * 0.05, KScreenWidth, KScreenHeight * 0.85);
    self.previewLayer.videoGravity = AVLayerVideoGravityResizeAspectFill;
    
    [self.view.layer addSublayer:self.previewLayer];

    
    //开始启动
    [self.session startRunning];
    
    
    //修改设备的属性，先加锁
    if ([self.device lockForConfiguration:nil]) {
        
        //自动白平衡
        if ([self.device isWhiteBalanceModeSupported:AVCaptureWhiteBalanceModeAutoWhiteBalance]) {
            [self.device setWhiteBalanceMode:AVCaptureWhiteBalanceModeAutoWhiteBalance];
        }
        
        //闪光灯自动
        if ([self.device isFlashModeSupported:AVCaptureFlashModeAuto]) {
            [self.device setFlashMode:AVCaptureFlashModeAuto];
        }
        
        //解锁
        [self.device unlockForConfiguration];
    }
    
}

- (void)initSubViews
{
    
    self.photoButton = [UIButton new];
    
    self.photoButton.frame = CGRectMake(AutoLayoutFunctionBtnSizeX(177), AutoLayoutFunctionBtnSizeY(671), AutoLayoutFunctionBtnWidth(60), AutoLayoutFunctionBtnHeight(60));
    [self.photoButton setImage:[UIImage imageNamed:@"photograph"] forState:UIControlStateNormal];
    [self.photoButton addTarget:self action:@selector(shutterCamera) forControlEvents:UIControlEventTouchUpInside];
    [self.view addSubview:self.photoButton];
    
    self.focusView = [[UIView alloc]initWithFrame:CGRectMake(0, 0, 80, 80)];
    self.focusView.layer.borderWidth = 1.0;
    self.focusView.layer.borderColor = [UIColor greenColor].CGColor;
    [self.view addSubview:self.focusView];
    self.focusView.hidden = YES;
    
//    self.flashButton = [UIButton buttonWithType:UIButtonTypeCustom];
//    self.flashButton.frame = CGRectMake(15, 15, 20, 20);
//    [self.flashButton setImage:[UIImage imageNamed:@"flashClose"] forState:UIControlStateNormal];
//
//    [self.flashButton addTarget:self action:@selector(changeFlash:) forControlEvents:UIControlEventTouchUpInside];
//    [self.view addSubview:self.flashButton];
    
    UIButton *leftButton = [UIButton buttonWithType:UIButtonTypeCustom];
    [leftButton setTitle:@"取消" forState:UIControlStateNormal];
    leftButton.titleLabel.textAlignment = NSTextAlignmentCenter;
    [leftButton sizeToFit];
    leftButton.center = CGPointMake((KScreenWidth - 220)/2.0/2.0, KScreenHeight-30);
    [leftButton addTarget:self action:@selector(disMiss) forControlEvents:UIControlEventTouchUpInside];
    [self.view addSubview:leftButton];
    
    
    self.flashButton = [UIButton buttonWithType:UIButtonTypeCustom];
    [ self.flashButton setTitle:@"切换" forState:UIControlStateNormal];
    self.flashButton.titleLabel.textAlignment = NSTextAlignmentCenter;
    [self.flashButton sizeToFit];
    self.flashButton.center = CGPointMake(KScreenWidth - (KScreenWidth - 220)/2.0/2.0, KScreenHeight-30);
    [ self.flashButton addTarget:self action:@selector(changeCamera) forControlEvents:UIControlEventTouchUpInside];
    [self.view addSubview: self.flashButton];
    
    UITapGestureRecognizer *tapGesture = [[UITapGestureRecognizer alloc]initWithTarget:self action:@selector(focusGesture:)];
    [self.view addGestureRecognizer:tapGesture];
    
}

- (void)focusGesture:(UITapGestureRecognizer*)gesture{
    CGPoint point = [gesture locationInView:gesture.view];
    [self focusAtPoint:point];
}
- (void)focusAtPoint:(CGPoint)point{
//    CGSize size = self.view.bounds.size;
    // focusPoint 函数后面Point取值范围是取景框左上角（0，0）到取景框右下角（1，1）之间,按这个来但位置就是不对，只能按上面的写法才可以。前面是点击位置的y/PreviewLayer的高度，后面是1-点击位置的x/PreviewLayer的宽度
    CGPoint focusPoint = CGPointMake(0, 1);
    
    if ([self.device lockForConfiguration:nil]) {
        
        if ([self.device isFocusModeSupported:AVCaptureFocusModeAutoFocus]) {
            [self.device setFocusPointOfInterest:focusPoint];
            [self.device setFocusMode:AVCaptureFocusModeAutoFocus];
        }
        
//        if ([self.device isExposureModeSupported:AVCaptureExposureModeAutoExpose ]) {
//            [self.device setExposurePointOfInterest:focusPoint];
//            //曝光量调节
//            [self.device setExposureMode:AVCaptureExposureModeAutoExpose];
//        }

        
        [self.device unlockForConfiguration];
        _focusView.center = point;
        _focusView.hidden = NO;
        [UIView animateWithDuration:0.2 animations:^{
            _focusView.transform = CGAffineTransformMakeScale(1.25, 1.25);
        }completion:^(BOOL finished) {
            [UIView animateWithDuration:0.2 animations:^{
                _focusView.transform = CGAffineTransformIdentity;
            } completion:^(BOOL finished) {
                _focusView.hidden = YES;
            }];
        }];
    }
    
}

- (void)FlashOn{
    
    if ([_device lockForConfiguration:nil]) {
        if (_isflashOn) {
            if ([_device isFlashModeSupported:AVCaptureFlashModeOff]) {
                [_device setFlashMode:AVCaptureFlashModeOff];
                _isflashOn = NO;
                [_flashButton setTitle:@"闪光灯关" forState:UIControlStateNormal];
            }
        }else{
            if ([_device isFlashModeSupported:AVCaptureFlashModeOn]) {
                [_device setFlashMode:AVCaptureFlashModeOn];
                _isflashOn = YES;
                [_flashButton setTitle:@"闪光灯开" forState:UIControlStateNormal];
            }
        }
        
        [_device unlockForConfiguration];
    }
}

- (void)changeCamera{
    //获取摄像头的数量
    NSUInteger cameraCount = [[AVCaptureDevice devicesWithMediaType:AVMediaTypeVideo] count];
    
    //摄像头小于等于1的时候直接返回
    if (cameraCount <= 1) return;
    
    AVCaptureDevice *newCamera = nil;
    AVCaptureDeviceInput *newInput = nil;
    //获取当前相机的方向(前还是后)
    AVCaptureDevicePosition position = [[self.input device] position];
    
    //为摄像头的转换加转场动画
    CATransition *animation = [CATransition animation];
    animation.timingFunction = [CAMediaTimingFunction functionWithName:kCAMediaTimingFunctionEaseInEaseOut];
    animation.duration = 0.5;
    animation.type = @"oglFlip";
    
    if (position == AVCaptureDevicePositionFront) {
        //获取后置摄像头
        newCamera = [self cameraWithPosition:AVCaptureDevicePositionBack];
        animation.subtype = kCATransitionFromLeft;
    }else{
        //获取前置摄像头
        newCamera = [self cameraWithPosition:AVCaptureDevicePositionFront];
        animation.subtype = kCATransitionFromRight;
    }
    
    [self.previewLayer addAnimation:animation forKey:nil];
    //输入流
    newInput = [AVCaptureDeviceInput deviceInputWithDevice:newCamera error:nil];
    
    
    if (newInput != nil) {
        
        [self.session beginConfiguration];
        //先移除原来的input
        [self.session removeInput:self.input];
        
        if ([self.session canAddInput:newInput]) {
            [self.session addInput:newInput];
            self.input = newInput;
            
        } else {
            //如果不能加现在的input，就加原来的input
            [self.session addInput:self.input];
        }
        
        [self.session commitConfiguration];
        
    }
    
    
}

- (AVCaptureDevice *)cameraWithPosition:(AVCaptureDevicePosition)position{
    NSArray *devices = [AVCaptureDevice devicesWithMediaType:AVMediaTypeVideo];
    for ( AVCaptureDevice *device in devices )
        if ( device.position == position ) return device;
    return nil;
}


#pragma mark- 拍照
- (void)shutterCamera
{
    AVCaptureConnection * videoConnection = [self.ImageOutPut connectionWithMediaType:AVMediaTypeVideo];
    
    UIDeviceOrientation curDeviceOrientation = [[UIDevice currentDevice] orientation];
    AVCaptureVideoOrientation avcaptureOrientation = [self avOrientationForDeviceOrientation:curDeviceOrientation];
    
    [videoConnection setVideoOrientation:avcaptureOrientation];
    [videoConnection setVideoScaleAndCropFactor:1.0];
    
    
    if (videoConnection ==  nil) {
        return;
    }

    
    __weak typeof(self) weak = self;
    [self.ImageOutPut captureStillImageAsynchronouslyFromConnection:videoConnection completionHandler:^(CMSampleBufferRef imageDataSampleBuffer, NSError *error) {
        
        if (imageDataSampleBuffer == nil) {
            return;
        }
        
        if (avcaptureOrientation == 3 || avcaptureOrientation == 4 || [self.directionStr isEqualToString:@"left"] || [self.directionStr isEqualToString:@"right"]) {
            NSData *imageData =  [AVCaptureStillImageOutput jpegStillImageNSDataRepresentation:imageDataSampleBuffer];
            UIImage *originImage = [[UIImage alloc] initWithData:imageData];
            
            ShowImageVC *showVC = [[ShowImageVC alloc]init];
            showVC.dataImage = originImage;
            showVC.location = self.location;
            showVC.name = self.name;
            showVC.superVC = self;
            showVC.isAcross = YES;
            [self.deviceMotion startMonitor];
            showVC.modalPresentationStyle = UIModalPresentationFullScreen;
            [self presentViewController:showVC animated:YES completion:nil];

        }
        else {
            NSData *imageData =  [AVCaptureStillImageOutput jpegStillImageNSDataRepresentation:imageDataSampleBuffer];
            UIImage *originImage = [[UIImage alloc] initWithData:imageData];
            NSLog(@"originImage=%@",originImage);
            CGFloat squareLength = weak.previewLayer.bounds.size.width;
            CGFloat previewLayerH = weak.previewLayer.bounds.size.height;
            CGSize size = CGSizeMake(squareLength * 2, previewLayerH * 2);
            UIImage *scaledImage = [originImage resizedImageWithContentMode:UIViewContentModeScaleAspectFill bounds:size interpolationQuality:kCGInterpolationHigh];
            NSLog(@"scaledImage=%@",scaledImage);
            CGRect cropFrame = CGRectMake((scaledImage.size.width - size.width) / 2, (scaledImage.size.height - size.height) / 2, size.width, size.height);
            NSLog(@"cropFrame:%@", [NSValue valueWithCGRect:cropFrame]);
            UIImage *croppedImage = [scaledImage croppedImage:cropFrame];
            NSLog(@"croppedImage=%@",croppedImage);
            
            ShowImageVC *showVC = [[ShowImageVC alloc]init];
            showVC.dataImage = croppedImage;
            showVC.location = self.location;
            showVC.name = self.name;
            showVC.superVC = self;
            showVC.isAcross = NO;
            [self.deviceMotion startMonitor];
            showVC.modalPresentationStyle = UIModalPresentationFullScreen;
            [self presentViewController:showVC animated:YES completion:nil];
        }
       
    }];
}

- (AVCaptureVideoOrientation)avOrientationForDeviceOrientation:(UIDeviceOrientation)deviceOrientation
{
    AVCaptureVideoOrientation result = (AVCaptureVideoOrientation)deviceOrientation;
    if ( deviceOrientation == UIDeviceOrientationLandscapeLeft || [self.directionStr isEqualToString:@"left"] )
        result = AVCaptureVideoOrientationLandscapeRight;
    else if ( deviceOrientation == UIDeviceOrientationLandscapeRight || [self.directionStr isEqualToString:@"right"] )
        result = AVCaptureVideoOrientationLandscapeLeft;
    return result;
}


/**
 * 保存图片到相册
 */
- (void)saveImageWithImage:(UIImage *)image {
    // 判断授权状态
    [PHPhotoLibrary requestAuthorization:^(PHAuthorizationStatus status) {
        if (status != PHAuthorizationStatusAuthorized) return;
        
        dispatch_async(dispatch_get_main_queue(), ^{
            NSError *error = nil;
            
            // 保存相片到相机胶卷
            __block PHObjectPlaceholder *createdAsset = nil;
            [[PHPhotoLibrary sharedPhotoLibrary] performChangesAndWait:^{
                createdAsset = [PHAssetCreationRequest creationRequestForAssetFromImage:image].placeholderForCreatedAsset;
            } error:&error];
            
            if (error) {
                NSLog(@"保存失败：%@", error);
                return;
            }
        });
    }];
}




- (void)disMiss {
    [self dismissViewControllerAnimated:YES completion:nil];
}

- (void)changeFlash:(UIButton *)sender {
    
    sender.selected = !sender.selected;
    
    if (self.flag == 0) {
        [sender setImage:[UIImage imageNamed:@"flashAuto"] forState:UIControlStateNormal];
        UILabel *remind = [[UILabel alloc]initWithFrame:CGRectMake(AutoLayoutFunctionBtnSizeX(155),AutoLayoutFunctionBtnSizeY(350), AutoLayoutFunctionBtnWidth(104), AutoLayoutFunctionBtnHeight(37))];
        remind.text = @"自动闪光灯";
        remind.textAlignment = NSTextAlignmentCenter;
        remind.textColor = [UIColor whiteColor];
        remind.font = [UIFont systemFontOfSize:15];
        remind.backgroundColor = [UIColor blackColor];
        remind.layer.cornerRadius = 5;
        remind.clipsToBounds = YES;
        [self.view addSubview:remind];
        [UIView animateWithDuration:1.5f animations:^{
            remind.alpha = 0.0f;
        }];
        
        if ([_device lockForConfiguration:nil]) {
            //闪光灯自动
            if ([_device isFlashModeSupported:AVCaptureFlashModeAuto]) {
                [_device setFlashMode:AVCaptureFlashModeAuto];
            }
            //自动白平衡
            if ([_device isWhiteBalanceModeSupported:AVCaptureWhiteBalanceModeAutoWhiteBalance]) {
                [_device setWhiteBalanceMode:AVCaptureWhiteBalanceModeAutoWhiteBalance];
            }
            //解锁
            [_device unlockForConfiguration];
        }
        self.flag++;
        return;
    }
    if (self.flag == 1) {
        [sender setImage:[UIImage imageNamed:@"flashOpen"] forState:UIControlStateNormal];
        UILabel *remind = [[UILabel alloc]initWithFrame:CGRectMake(AutoLayoutFunctionBtnSizeX(155),AutoLayoutFunctionBtnSizeY(350), AutoLayoutFunctionBtnWidth(104), AutoLayoutFunctionBtnHeight(37))];
        remind.text = @"闪光灯开启";
        remind.textAlignment = NSTextAlignmentCenter;
        remind.textColor = [UIColor whiteColor];
        remind.font = [UIFont systemFontOfSize:15];
        remind.backgroundColor = [UIColor blackColor];
        remind.layer.cornerRadius = 5;
        remind.clipsToBounds = YES;
        [self.view addSubview:remind];
        [UIView animateWithDuration:1.5f animations:^{
            remind.alpha = 0.0f;
        }];
        if ([_device lockForConfiguration:nil]) {
            if ([_device isFlashModeSupported:AVCaptureFlashModeOn]) {
                [_device setFlashMode:AVCaptureFlashModeOn];
            }
            [_device unlockForConfiguration];
        }
        self.flag++;
        return;
    }
    if (self.flag == 2) {
        [sender setImage:[UIImage imageNamed:@"flashClose"] forState:UIControlStateNormal];
        UILabel *remind = [[UILabel alloc]initWithFrame:CGRectMake(AutoLayoutFunctionBtnSizeX(155),AutoLayoutFunctionBtnSizeY(350), AutoLayoutFunctionBtnWidth(104), AutoLayoutFunctionBtnHeight(37))];
        remind.text = @"闪光灯关闭";
        remind.textAlignment = NSTextAlignmentCenter;
        remind.textColor = [UIColor whiteColor];
        remind.font = [UIFont systemFontOfSize:15];
        remind.backgroundColor = [UIColor blackColor];
        remind.layer.cornerRadius = 5;
        remind.clipsToBounds = YES;
        [self.view addSubview:remind];
        [UIView animateWithDuration:1.5f animations:^{
            remind.alpha = 0.0f;
        }];
        if ([_device lockForConfiguration:nil]) {
            if ([_device isFlashModeSupported:AVCaptureFlashModeOff]) {
                [_device setFlashMode:AVCaptureFlashModeOff];
            }
            [_device unlockForConfiguration];
        }
        self.flag = 0;
        return ;
    }
}


#pragma mark- 检测相机权限
- (BOOL)checkCameraPermission
{
    AVAuthorizationStatus authStatus = [AVCaptureDevice authorizationStatusForMediaType:AVMediaTypeVideo];
    if (authStatus == AVAuthorizationStatusDenied) {
        UIAlertView *alertView = [[UIAlertView alloc]initWithTitle:@"请打开相机权限" message:@"设置-隐私-相机" delegate:self cancelButtonTitle:@"确定" otherButtonTitles:@"取消", nil];
        alertView.tag = 100;
        [alertView show];
        return NO;
    }
    else{
        return YES;
    }
    return YES;
}

- (void)alertView:(UIAlertView *)alertView clickedButtonAtIndex:(NSInteger)buttonIndex{
    if (buttonIndex == 0 && alertView.tag == 100) {
        
        NSURL * url = [NSURL URLWithString:UIApplicationOpenSettingsURLString];
        
        if([[UIApplication sharedApplication] canOpenURL:url]) {
            
            [[UIApplication sharedApplication] openURL:url];
            
        }
    }
    
    if (buttonIndex == 1 && alertView.tag == 100) {
        
        [self disMiss];
    }
    
}



@end
