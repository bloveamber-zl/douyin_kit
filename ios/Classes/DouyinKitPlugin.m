#import "DouyinKitPlugin.h"
#import <DouyinOpenSDK/DouyinOpenSDKApplicationDelegate.h>
#import <DouyinOpenSDK/DouyinOpenSDKAuth.h>
#import <DouyinOpenSDK/DouyinOpenSDKShare.h>
#import <KwaiSDK/KSApi.h>
#import <Photos/Photos.h>

@interface DouyinKitPlugin () <KSApiDelegate>
@end

@implementation DouyinKitPlugin {
    FlutterMethodChannel *_channel;
}

+ (void)registerWithRegistrar:(NSObject<FlutterPluginRegistrar> *)registrar {
    FlutterMethodChannel *channel =
        [FlutterMethodChannel methodChannelWithName:@"v7lin.github.io/douyin_kit"
                                    binaryMessenger:[registrar messenger]];
    DouyinKitPlugin *instance = [[DouyinKitPlugin alloc] initWithChannel:channel];
    [registrar addApplicationDelegate:instance];
    [registrar addMethodCallDelegate:instance channel:channel];
}

- (instancetype)initWithChannel:(FlutterMethodChannel *)channel {
    self = [super init];
    if (self) {
        _channel = channel;
    }
    return self;
}

- (void)handleMethodCall:(FlutterMethodCall *)call
                  result:(FlutterResult)result {
    if ([@"registerApp" isEqualToString:call.method]) {
        NSString *clientKey = call.arguments[@"client_key"];
        // reg
         [KSApi registerApp:@"ks691302544702903037" universalLink:@"https://lepai-api.faceqianyan.com/app/" delegate:self];

        [[DouyinOpenSDKApplicationDelegate sharedInstance] registerAppId:clientKey];

        result(nil);
    } else if ([@"isInstalled" isEqualToString:call.method]) {
        result([NSNumber numberWithBool:[[DouyinOpenSDKApplicationDelegate sharedInstance] isAppInstalled]]);
    } else if ([@"isSupportAuth" isEqualToString:call.method]) {
        result([NSNumber numberWithBool:YES]);
    } else if ([@"auth" isEqualToString:call.method]) {
        [self handleAuthCall:call result:result];
    } else if ([@"isSupportShare" isEqualToString:call.method]) {
        
    } else if ([@[@"shareImage", @"shareVideo", @"shareMicroApp", @"shareHashTags", @"shareAnchor"] containsObject:call.method]) {
        [self handleShareCall:call result:result]; // call.argument("video_uris")  call.arguments
    } else if ([@"isSupportShareToContacts" isEqualToString:call.method]) {
        
    } else if ([@[@"shareImageToContacts", @"shareHtmlToContacts"] containsObject:call.method]) {
        [self handleShareToContactsCall:call result:result];
    } else if ([@"isSupportOpenRecord" isEqualToString:call.method]) {
        
    } else if ([@"openRecord" isEqualToString:call.method]) {
        [self handleOpenRecordCall:call result:result];
    } else if ([@[@"ksShareVideo"] containsObject:call.method]) {
        [self handleKSShareCall:call result:result]; // call.argument("video_uris")  call.arguments
    } else if ([@[@"ksShareImage"] containsObject:call.method]) {
        [self handleKSShareImageCall:call result:result]; // call.argument("image_uris")  call.arguments
    } else if ([@"isSupportShareToContacts" isEqualToString:call.method]) {
    } else {
        result(FlutterMethodNotImplemented);
    }
}

- (void)handleAuthCall:(FlutterMethodCall *)call result:(FlutterResult)result {
}


- (void)handleKSShareCall:(FlutterMethodCall *)call
                      result:(FlutterResult)result {
    NSDictionary *arg = call.arguments;
    if ([arg isKindOfClass:NSDictionary.class]) {
        NSArray *uris = arg[@"video_uris"];
        if ([uris isKindOfClass:NSArray.class]) {
            __block NSMutableArray *assetLocalIds = [NSMutableArray array];
            [[PHPhotoLibrary sharedPhotoLibrary] performChanges:^{
                
                NSURL *url = [NSURL URLWithString:uris.firstObject]; // file://
                PHAssetChangeRequest *request = [PHAssetChangeRequest creationRequestForAssetFromVideoAtFileURL:url];
               NSString *localId = request.placeholderForCreatedAsset.localIdentifier;
               [assetLocalIds addObject:localId];
                
            } completionHandler:^(BOOL success, NSError * _Nullable error) {
                if (success) {
                   dispatch_async(dispatch_get_main_queue(), ^{
                       
                       
                       KSShareMediaAsset *asset = [KSShareMediaAsset assetForPhotoLibrary:assetLocalIds.firstObject isImage:NO];
                       KSShareMediaObject *object = [[KSShareMediaObject alloc] init];
                       object.multipartAssets = @[asset];
                       //object 参数配置
                       KSShareMediaRequest *request = [[KSShareMediaRequest alloc] init];
                       request.mediaFeature = KSShareMediaFeature_VideoPublish;
                       request.mediaObject = object;
                       [KSApi sendRequest:request completion:^(BOOL success) {
                           if (result) {
                               result(nil);
                           }
                           NSLog(@"kuaishou share = %d", success);
                       }];
                   });
                }
            }];
            return;
        }
    }
    if (result) {
        result(nil);
    }
}

- (void)handleKSShareImageCall:(FlutterMethodCall *)call
                      result:(FlutterResult)result {
    NSDictionary *arg = call.arguments;
    if ([arg isKindOfClass:NSDictionary.class]) {
        NSArray *uris = arg[@"image_uris"];
        if ([uris isKindOfClass:NSArray.class]) {
            __block NSMutableArray *assetLocalIds = [NSMutableArray array];
            [[PHPhotoLibrary sharedPhotoLibrary] performChanges:^{
                
                NSURL *url = [NSURL URLWithString:uris.firstObject]; // file://
                PHAssetChangeRequest *request = [PHAssetChangeRequest creationRequestForAssetFromImageAtFileURL:url];
               NSString *localId = request.placeholderForCreatedAsset.localIdentifier;
               [assetLocalIds addObject:localId];
                
            } completionHandler:^(BOOL success, NSError * _Nullable error) {
                if (success) {
                   dispatch_async(dispatch_get_main_queue(), ^{
                       
                       
                       KSShareMediaAsset *asset = [KSShareMediaAsset assetForPhotoLibrary:assetLocalIds.firstObject isImage:YES];
                       KSShareMediaObject *object = [[KSShareMediaObject alloc] init];
                       object.multipartAssets = @[asset];
                       //object 参数配置
                       KSShareMediaRequest *request = [[KSShareMediaRequest alloc] init];
                       request.mediaFeature = KSShareMediaFeature_PictureEdit;
                       request.mediaObject = object;
                       [KSApi sendRequest:request completion:^(BOOL success) {
                           if (result) {
                               result(nil);
                           }
                           NSLog(@"kuaishou share = %d", success);
                       }];
                   });
                }
            }];
            return;
        }
    }
    if (result) {
        result(nil);
    }
}

// - (void)handleKSShareImageCall:(FlutterMethodCall *)call
//                       result:(FlutterResult)result {
//     NSDictionary *arg = call.arguments;
    
//     if ([arg isKindOfClass:NSDictionary.class]) {
//         NSArray *uris = arg[@"image_uris"];
//          NSLog(@"firstObject %@", uris.firstObject);
//         // NSURL *imageUrl = [NSURL fileURLWithPath:uris.firstObject];
//         NSURL *imageUrl = [NSURL URLWithString:uris.firstObject];
//          NSLog(@"test 1");
//         __block NSString *localId;
//         [[PHPhotoLibrary sharedPhotoLibrary]performChanges:^{
//             // 这部分在后台线程执行
//             PHAssetCreationRequest *req = [PHAssetCreationRequest creationRequestForAssetFromImageAtFileURL:imageUrl];
//             // 获取刚才添加的相片的本地标识
//             localId = req.placeholderForCreatedAsset.localIdentifier;
//               NSLog(@"test 2");

//         }completionHandler:^(BOOL success, NSError * _Nullable error) {
//               NSLog(@"test 3 :%@", localId);
//               NSLog(@"success = %d", success);

             


//             // 这部分在主线程执行
//             if (success) {

//                 dispatch_async(dispatch_get_main_queue(), ^{

//                     //  PHFetchResult *assetsresult = [PHAsset fetchAssetsWithLocalIdentifiers:@[localId] options:nil];
//                     //  PHAsset *asset = [assetsresult firstObject];



//                     NSMutableArray<KSShareMediaAsset *> *shareItems = [NSMutableArray arrayWithCapacity:1];
//                     BOOL isImage = YES;
//                     [shareItems addObject:[KSShareMediaAsset assetForPhotoLibrary:localId isImage:isImage]];

//                     KSShareMediaAsset *thumbnailItem = [KSShareMediaAsset assetForPhotoLibrary:localId isImage:isImage];
                
//                     NSLog(@"localId： %@",localId);


//                     KSShareMediaObject *mediaItem = [[KSShareMediaObject alloc] init];
//                     //建议为NO, 即执行兜底逻辑，无相关发布权限时进入预裁剪页
//                     mediaItem.disableFallback = NO;
//                     //暂不对外开放，不要用
//                     mediaItem.extraEntity = nil;
//                     //发快手关联的标签
//                     mediaItem.tags = @[];

//                     //选择发布的封面图片
//                     mediaItem.coverAsset = thumbnailItem;
//                     //选择发布的素材，图片，视频，多段视频等，需要与mediaFeature相符
//                     mediaItem.multipartAssets = shareItems;

//                     KSShareMediaRequest *request = [[KSShareMediaRequest alloc] init];

//                     // //目前分享发布视频支持以下类型
//                     // //KSShareMediaFeature_Preprocess ///< 裁剪功能，跳转到分享视频裁剪页。
//                     // //KSShareMediaFeature_VideoEdit ///< 视频编辑功能，跳转到分享视频编辑页，该能力需要申请权限
//                     // //KSShareMediaFeature_PictureEdit ///< 图片编辑功能，跳转到分享视频图片编辑页
//                     // //KSShareMediaFeature_VideoPublish ///< 视频发布功能，跳转到分享视频发布功能页，该能力需要申请权限
//                     // //KSShareMediaFeature_AICut ///< 智能裁剪功能
//                     // // 如果没有权限或者视频超长等原因，可能会降级到视频裁剪页。
//                     request.mediaFeature = KSShareMediaFeature_PictureEdit;
//                     request.mediaObject = mediaItem;
//                     __weak __typeof(self) ws = self;


//                     // KSShareMediaObject *mediaItem = [[KSShareMediaObject alloc] init];
//                     //  NSLog(@"test 2");
//                     // //建议为NO, 即执行兜底逻辑，无相关发布权限时进入预裁剪页
//                     // mediaItem.disableFallback = YES;
//                     // mediaItem.extraEntity =  nil;
//                     // NSLog(@"test 3");
//     //                 mediaItem.tags = @[];
                    
//     //                 mediaItem.coverAsset = thumbnailItem;
//     //                 NSLog(@"test 4");
//     //                 mediaItem.multipartAssets = shareItems;
//     //                 NSLog(@"test 5");
                    
//     // //                mediaItem.associateType = shareMedia.associateType;
//     // //                mediaItem.associateObject = shareMedia.associateObject;
                        
//     //                 KSShareMediaRequest *request = [[KSShareMediaRequest alloc] init];
//     //                 NSLog(@"test 6");
//     // //                request.applicationList = [self.applicationList copy];
//     //                 request.mediaFeature = KSShareMediaFeature_PictureEdit;
//     //                 NSLog(@"test 7");
//     //                 request.mediaObject = mediaItem;
//     //                 NSLog(@"test 8");
//     //                   __weak __typeof(self) ws = self;
//     //                 [KSApi sendRequest:request completion:^(BOOL success) {
//     //                     NSLog(@"kuaishou share = %d", success);
//     // //                    __strong __typeof(ws) ss = ws;
//     // //                    [ss logFormat:@"%s success: %@", __func__, success ? @"YES" : @"NO"];
//     // //                    completion ? completion(success) : nil;
//     //                 }];
                    

//                     [KSApi sendRequest:request completion:^(BOOL success) {
//                         // if (result) {
//                         //     result(nil);
//                         // }
//                         NSLog(@"kuaishou share111", success);
//                         NSLog(@"kuaishou share = %d", success);
//                     }];

//                     // asset便是所需要的PHAsset对象
                
//                });


//             }
//         }];





//         // if ([uris isKindOfClass:NSArray.class]) {
//         //     __block NSMutableArray *assetLocalIds = [NSMutableArray array];
//         //     // __block NSString *localId;
//         //     [[PHPhotoLibrary sharedPhotoLibrary] performChanges:^{
                
//         //         NSURL *url = [NSURL URLWithString:uris.firstObject]; // file://
//         //         PHAssetChangeRequest *request = [PHAssetChangeRequest creationRequestForAssetFromImageAtFileURL:url];
//         //         NSString *localId = request.placeholderForCreatedAsset.localIdentifier;
//         //        [assetLocalIds addObject:localId];

                
//         //     } completionHandler:^(BOOL success, NSError * _Nullable error) {
//         //         if (success) {
//         //            dispatch_async(dispatch_get_main_queue(), ^{
//         //                 NSLog(@"test 1");
//         //                KSShareMediaAsset *asset = [KSShareMediaAsset assetForPhotoLibrary:assetLocalIds.firstObject isImage:YES];
//         //                KSShareMediaObject *object = [[KSShareMediaObject alloc] init];
//         //                object.disableFallback = NO;
//         //                 NSLog(@"localId111： %@", assetLocalIds.firstObject);

//         //                 // NSMutableArray<KSShareMediaAsset *> *shareItems = [NSMutableArray arrayWithCapacity:1];
//         //                 // [shareItems addObject:[KSShareMediaAsset assetForPhotoLibrary:localId isImage:YES]];

//         //             //    object.multipartAssets = shareItems;
//         //                object.multipartAssets = @[asset];
//         //                  NSLog(@"test 2");

//         //                //object 参数配置
//         //                KSShareMediaRequest *request = [[KSShareMediaRequest alloc] init];
//         //                request.mediaFeature = KSShareMediaFeature_PictureEdit;
//         //                request.mediaObject = object;
//         //                   NSLog(@"test 3");
//         //                [KSApi sendRequest:request completion:^(BOOL success) {
//         //                    if (result) {
//         //                        result(nil);
//         //                    }
//         //                     NSLog(@"kuaishou share111", success);
//         //                    NSLog(@"kuaishou share = %d", success);
//         //                }];
//         //            });
//         //         }
//         //     }];
//         //     return;
//         // }
//     }
//     if (result) {
//         result(nil);
//     }
// }


// - (void)handleKSShareImageCall:(FlutterMethodCall *)call
//                       result:(FlutterResult)result {
//     NSDictionary *arg = call.arguments;
//     if ([arg isKindOfClass:NSDictionary.class]) {
//         NSArray *uris = arg[@"image_uris"];
//         if ([uris isKindOfClass:NSArray.class]) {
//             __block NSMutableArray *assetLocalIds = [NSMutableArray array];
//             // __block NSString *localId;
//             [[PHPhotoLibrary sharedPhotoLibrary] performChanges:^{
                
//                 NSURL *url = [NSURL URLWithString:uris.firstObject]; // file://
//                 PHAssetChangeRequest *request = [PHAssetChangeRequest creationRequestForAssetFromImageAtFileURL:url];
//                 NSString *localId = request.placeholderForCreatedAsset.localIdentifier;
//                [assetLocalIds addObject:localId];

//                 // KSShareMediaAsset *asset = [KSShareMediaAsset assetForPhotoLibrary:assetLocalIds.firstObject isImage:YES];
                
//             } completionHandler:^(BOOL success, NSError * _Nullable error) {
//                 if (success) {
//                    dispatch_async(dispatch_get_main_queue(), ^{
//                         NSLog(@"test 1");
//                        KSShareMediaAsset *asset = [KSShareMediaAsset assetForPhotoLibrary:assetLocalIds.firstObject isImage:YES];
//                        KSShareMediaObject *object = [[KSShareMediaObject alloc] init];
//                        object.disableFallback = NO;
//                         NSLog(@"localId111： %@", assetLocalIds.firstObject);

//                         // NSMutableArray<KSShareMediaAsset *> *shareItems = [NSMutableArray arrayWithCapacity:1];
//                         // [shareItems addObject:[KSShareMediaAsset assetForPhotoLibrary:localId isImage:YES]];

//                     //    object.multipartAssets = shareItems;
//                        object.multipartAssets = @[asset];
//                          NSLog(@"test 2");

//                        //object 参数配置
//                        KSShareMediaRequest *request = [[KSShareMediaRequest alloc] init];
//                        request.mediaFeature = KSShareMediaFeature_PictureEdit;
//                        request.mediaObject = object;
//                           NSLog(@"test 3");
//                        [KSApi sendRequest:request completion:^(BOOL success) {
//                            if (result) {
//                                result(nil);
//                            }
//                             NSLog(@"kuaishou share111", success);
//                            NSLog(@"kuaishou share = %d", success);
//                        }];
//                    });
//                 }
//             }];
//             return;
//         }
//     }
//     if (result) {
//         result(nil);
//     }
// }

- (void)handleShareCall:(FlutterMethodCall *)call
                      result:(FlutterResult)result {
    NSDictionary *arg = call.arguments;
    NSLog(@"test arg=%@", arg);
    if ([arg isKindOfClass:NSDictionary.class]) {
        NSArray *uris;
        DouyinOpenSDKShareMediaType type;

        if ([@"shareImage" isEqualToString:call.method]) {
            uris = arg[@"image_uris"];
            type = DouyinOpenSDKShareMediaTypeImage;
        } else {
            uris = arg[@"video_uris"];
            type = DouyinOpenSDKShareMediaTypeVideo;
        }


        NSLog(@"test uris=%@", uris);
        if ([uris isKindOfClass:NSArray.class]) {
            __block NSMutableArray *assetLocalIds = [NSMutableArray array];
            [[PHPhotoLibrary sharedPhotoLibrary] performChanges:^{
                
                NSURL *url = [NSURL URLWithString:uris.firstObject]; // file://
               PHAssetChangeRequest *request;
                if ([@"shareImage" isEqualToString:call.method]) {
                    request = [PHAssetChangeRequest creationRequestForAssetFromImageAtFileURL:url];
                } else {
                    request = [PHAssetChangeRequest creationRequestForAssetFromVideoAtFileURL:url];
                }

                NSString *localId = request.placeholderForCreatedAsset.localIdentifier;
                [assetLocalIds addObject:localId];

                NSLog(@"localId = %@", localId);

                
            } completionHandler:^(BOOL success, NSError * _Nullable error) {
                if (success) {
                   dispatch_async(dispatch_get_main_queue(), ^{
                       
                        DouyinOpenSDKShareRequest *req = [[DouyinOpenSDKShareRequest alloc] init];
                        req.mediaType = type; 
                        req.landedPageType = DouyinOpenSDKLandedPageEdit;    // 设置分享的目标页面
                        req.localIdentifiers = assetLocalIds;

                        req.title = [DouyinOpenSDKShareTitle new];
                        req.title.text = arg[@"title"];

                        NSArray *stringArray = arg[@"hash_tags"];
                        for (NSString *string in stringArray) {
                            // 在这里对每个字符串进行操作
                            DouyinOpenSDKTitleHashtag *hashtag = [DouyinOpenSDKTitleHashtag new];
                            hashtag.text = string;
                            [req.title.hashtags addObject:hashtag];
                        }

                       [req sendShareRequestWithCompleteBlock:^(DouyinOpenSDKShareResponse * _Nonnull respond) {
                           if (respond.isSucceed) {
                           // Share Succeed
                           } else{

                               NSLog(@"respond = %@", respond.errString);

                           }
                           if (result) {
                               result(nil);
                           }
                       }];
                   });
                }
            }];
        }
    }
    if (result) {
        result(nil);
    }
}

- (void)handleShareToContactsCall:(FlutterMethodCall *)call
                      result:(FlutterResult)result {
}

- (void)handleOpenRecordCall:(FlutterMethodCall *)call
                      result:(FlutterResult)result {
}

#pragma mark - AppDelegate

- (BOOL)application:(UIApplication *)application handleOpenURL:(NSURL *)url {
    BOOL ret = [[DouyinOpenSDKApplicationDelegate sharedInstance] application:application openURL:url sourceApplication:nil annotation:nil];
    if (!ret) {
        ret = [KSApi handleOpenURL:url];
    }
    return ret;
}

- (BOOL)application:(UIApplication *)application
              openURL:(NSURL *)url
    sourceApplication:(NSString *)sourceApplication
           annotation:(id)annotation {
    BOOL ret = [[DouyinOpenSDKApplicationDelegate sharedInstance] application:application openURL:url sourceApplication:sourceApplication annotation:annotation];
    if (!ret) {
        ret = [KSApi handleOpenURL:url];
    }
    return ret;
}

- (BOOL)application:(UIApplication *)application
            openURL:(NSURL *)url
            options:
                (NSDictionary<UIApplicationOpenURLOptionsKey, id> *)options {
    BOOL ret = [[DouyinOpenSDKApplicationDelegate sharedInstance] application:application openURL:url sourceApplication:options[UIApplicationOpenURLOptionsSourceApplicationKey] annotation:options[UIApplicationOpenURLOptionsAnnotationKey]];
    if (!ret) {
        ret = [KSApi handleOpenURL:url];
    }
    return ret;
}

- (BOOL)application:(UIApplication*)application
    continueUserActivity:(NSUserActivity*)userActivity
      restorationHandler:(void (^)(NSArray*))restorationHandler {
    return [KSApi handleOpenUniversalLink:userActivity];
}

@end
