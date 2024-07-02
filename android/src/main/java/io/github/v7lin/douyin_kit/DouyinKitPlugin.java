package io.github.v7lin.douyin_kit;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ProviderInfo;
import android.net.Uri;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.core.content.FileProvider;

import com.bytedance.sdk.open.aweme.authorize.model.Authorization;
import com.bytedance.sdk.open.aweme.base.AnchorObject;
import com.bytedance.sdk.open.aweme.base.ImageObject;
import com.bytedance.sdk.open.aweme.base.MediaContent;
import com.bytedance.sdk.open.aweme.base.MicroAppInfo;
import com.bytedance.sdk.open.aweme.base.ShareParam;
import com.bytedance.sdk.open.aweme.base.TitleObject;
import com.bytedance.sdk.open.aweme.base.VideoObject;
import com.bytedance.sdk.open.aweme.common.handler.IApiEventHandler;
import com.bytedance.sdk.open.aweme.common.model.BaseReq;
import com.bytedance.sdk.open.aweme.common.model.BaseResp;
import com.bytedance.sdk.open.aweme.share.Share;
import com.bytedance.sdk.open.douyin.DouYinOpenApiFactory;
import com.bytedance.sdk.open.douyin.DouYinOpenConfig;
import com.bytedance.sdk.open.douyin.ShareToContact;
import com.bytedance.sdk.open.douyin.api.DouYinOpenApi;
import com.bytedance.sdk.open.douyin.model.ContactHtmlObject;
import com.bytedance.sdk.open.douyin.model.OpenRecord;
import com.kwai.opensdk.sdk.constants.KwaiPlatform;
import com.kwai.opensdk.sdk.model.base.OpenSdkConfig;
import com.kwai.opensdk.sdk.model.postshare.PostShareMediaInfo;
import com.kwai.opensdk.sdk.model.postshare.SinglePicturePublish;
import com.kwai.opensdk.sdk.model.postshare.SingleVideoPublish;
import com.kwai.opensdk.sdk.openapi.IKwaiAPIEventListener;
import com.kwai.opensdk.sdk.openapi.IKwaiOpenAPI;
import com.kwai.opensdk.sdk.openapi.KwaiOpenAPI;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import io.flutter.embedding.engine.plugins.FlutterPlugin;
import io.flutter.embedding.engine.plugins.activity.ActivityAware;
import io.flutter.embedding.engine.plugins.activity.ActivityPluginBinding;
import io.flutter.plugin.common.MethodCall;
import io.flutter.plugin.common.MethodChannel;
import io.flutter.plugin.common.MethodChannel.MethodCallHandler;
import io.flutter.plugin.common.MethodChannel.Result;
import io.flutter.plugin.common.PluginRegistry;
import io.flutter.view.FlutterNativeView;



/**
 * DouyinKitPlugin
 */
public final class DouyinKitPlugin implements FlutterPlugin, ActivityAware, MethodCallHandler, PluginRegistry.ViewDestroyListener {
    /// The MethodChannel that will the communication between Flutter and native Android
    ///
    /// This local reference serves to register the plugin with the Flutter Engine and unregister it
    /// when the Flutter Engine is detached from the Activity
    private MethodChannel channel;
    private Context applicationContext;
    private Activity activity;

    private final AtomicBoolean register = new AtomicBoolean(false);

    //
    private DouYinOpenApi createOpenApi() {
        return activity != null ? DouYinOpenApiFactory.create(activity) : null;
    }

    // --- FlutterPlugin

    @Override
    public void onAttachedToEngine(@NonNull FlutterPluginBinding binding) {
        channel = new MethodChannel(binding.getBinaryMessenger(), "v7lin.github.io/douyin_kit");
        channel.setMethodCallHandler(this);

        applicationContext = binding.getApplicationContext();

        if (register.compareAndSet(false, true)) {
            DouyinReceiver.registerReceiver(binding.getApplicationContext(), douyinReceiver);
        }
    }

    private DouyinReceiver douyinReceiver = new DouyinReceiver() {
        @Override
        public void handleIntent(Intent intent) {
            DouYinOpenApi openApi = createOpenApi();
            if (openApi != null) {
                openApi.handleIntent(intent, iApiEventHandler);
            }
        }
    };

    private IApiEventHandler iApiEventHandler = new IApiEventHandler() {
        @Override
        public void onReq(BaseReq req) {

        }

        @Override
        public void onResp(BaseResp resp) {
            Map<String, Object> map = new HashMap<>();
            map.put("error_code", resp.errorCode);
            map.put("error_msg", resp.errorMsg);
            if (resp.extras != null) {
                // TODO
            }
            if (resp instanceof Authorization.Response) {
                Authorization.Response authResp = (Authorization.Response) resp;
                map.put("auth_code", authResp.authCode);
                map.put("state", authResp.state);
                map.put("granted_permissions", authResp.grantedPermissions);
                if (channel != null) {
                    channel.invokeMethod("onAuthResp", map);
                }
            } else if (resp instanceof Share.Response) {
                Share.Response shareResp = (Share.Response) resp;
                map.put("state", shareResp.state);
                map.put("sub_error_code", shareResp.subErrorCode);
                if (channel != null) {
                    channel.invokeMethod("onShareResp", map);
                }
            } else if (resp instanceof ShareToContact.Response) {
                ShareToContact.Response shareToContactResp = (ShareToContact.Response) resp;
                map.put("state", shareToContactResp.mState);
                if (channel != null) {
                    channel.invokeMethod("onShareToContactResp", map);
                }
            } else if (resp instanceof OpenRecord.Response) {
                OpenRecord.Response openRecordResp = (OpenRecord.Response) resp;
                map.put("state", openRecordResp.state);
                if (channel != null) {
                    channel.invokeMethod("onOpenRecordResp", map);
                }
            }
        }

        @Override
        public void onErrorIntent(Intent intent) {
            // TODO
        }
    };

    @Override
    public void onDetachedFromEngine(@NonNull FlutterPluginBinding binding) {
        channel.setMethodCallHandler(null);
        channel = null;

        applicationContext = null;

        if (register.compareAndSet(true, false)) {
            DouyinReceiver.unregisterReceiver(binding.getApplicationContext(), douyinReceiver);


            // --- 快手 Start
            // 移除对回调结果的监听，请及时移除不用的监听避免内存泄漏问题
            if (mKwaiOpenAPI != null) {
                mKwaiOpenAPI.removeKwaiAPIEventListerer();
            }
        }
    }

    // --- ActivityAware

    @Override
    public void onAttachedToActivity(@NonNull ActivityPluginBinding binding) {
        activity = binding.getActivity();
    }

    @Override
    public void onDetachedFromActivityForConfigChanges() {
        onDetachedFromActivity();
    }

    @Override
    public void onReattachedToActivityForConfigChanges(@NonNull ActivityPluginBinding binding) {
        onAttachedToActivity(binding);
    }

    @Override
    public void onDetachedFromActivity() {
        activity = null;
    }

    // --- MethodCallHandler

    @Override
    public void onMethodCall(@NonNull MethodCall call, @NonNull Result result) {
        if ("registerApp".equals(call.method)) {

            // 这里初始化快手
            registerAppKs(call, result);

            registerApp(call, result);

        } else if ("isInstalled".equals(call.method)) {
            DouYinOpenApi openApi = createOpenApi();
            result.success(openApi != null && openApi.isAppInstalled());
        } else if ("isSupportAuth".equals(call.method)) {
            DouYinOpenApi openApi = createOpenApi();
            result.success(openApi != null && openApi.isAppSupportAuthorization());
        } else if ("auth".equals(call.method)) {
            handleAuthCall(call, result);
        } else if ("isSupportShare".equals(call.method)) {
            DouYinOpenApi openApi = createOpenApi();
            result.success(openApi != null && openApi.isAppSupportShare());
        } else if (Arrays.asList("shareImage", "shareVideo", "shareMicroApp", "shareHashTags", "shareAnchor").contains(call.method)) {
            Log.i("test","test 1");
            handleShareCall(call, result);
        } else if ("isSupportShareToContacts".equals(call.method)) {
            DouYinOpenApi openApi = createOpenApi();
            result.success(openApi != null && openApi.isAppSupportShareToContacts());
        } else if (Arrays.asList("shareImageToContacts", "shareHtmlToContacts").contains(call.method)) {
            handleShareToContactsCall(call, result);
        } else if ("isSupportOpenRecord".equals(call.method)) {
            DouYinOpenApi openApi = createOpenApi();
            result.success(openApi != null && openApi.isSupportOpenRecordPage());
        } else if ("openRecord".equals(call.method)) {
            handleOpenRecordCall(call, result);
        } else if ("ksShareVideo".equals(call.method)) {
           // 分享视频到快手
            handleShareVideoCallKs(call, result);
        } else if ("ksShareImage".equals(call.method)) {
            // 分享图片到快手
            handleShareImageCallKs(call, result);
        } else {
            result.notImplemented();
        }
    }

    // --- 快手 Start
    private IKwaiOpenAPI mKwaiOpenAPI; // 声明使用接口
    // 初始化
    private void registerAppKs(MethodCall call, MethodChannel.Result result) {

        mKwaiOpenAPI = new KwaiOpenAPI(activity);

        Log.i("SHARE----KS//", "registerAppKs");

        // 设置平台功能的配置选项
        OpenSdkConfig openSdkConfig = new OpenSdkConfig.Builder()
                .setGoToMargetAppNotInstall(true) // 应用未安装，是否自动跳转应用市场
                .setGoToMargetAppVersionNotSupport(true) // 应用已安装但版本不支持，是否自动跳转应用市场
                .setSetNewTaskFlag(true) // 设置启动功能页面是否使用新的页面栈
                .setSetClearTaskFlag(true) // 设置启动功能页面是否清除当前页面栈，当isSetNewTaskFlag为true时生效
                .setShowDefaultLoading(false) // 是否显示默认的loading页面作为功能启动的过渡
                .build();
        Log.i("SHARE----KS//", "registerAppKs2");
        mKwaiOpenAPI.setOpenSdkConfig(openSdkConfig);
   Log.i("SHARE----KS//", "registerAppKs3");
        // 业务请求回调结果监听
        mKwaiOpenAPI.addKwaiAPIEventListerer(new IKwaiAPIEventListener() {
            @Override
            public void onRespResult(@NonNull com.kwai.opensdk.sdk.model.base.BaseResp resp) {
                Log.i("SHARE----KS//", "resp=" + resp);
                if (resp != null) {
                    Log.i("SHARE----KS//", "errorCode=" + resp.errorCode + ", errorMsg="
                        + resp.errorMsg + ", cmd=" + resp.getCommand()
                        + ", transaction=" + resp.transaction + ", platform=" + resp.platform);
                } else {
                    Log.i("SHARE----KS//", "CallBackResult: resp is null");
                }
            }
        });

    }

    // 分享
    private void handleShareVideoCallKs(MethodCall call, MethodChannel.Result result) {
        if (mKwaiOpenAPI == null) return;

        Log.i("SHARE----KS//", "handleShareVideoCallKs");
        SingleVideoPublish.Req req = new SingleVideoPublish.Req();
        req.sessionId = mKwaiOpenAPI.getOpenAPISessionId();
        req.transaction = "SingleVideoPublish";
        // 设置功能调起快手支持应用，KwaiPlatform.Platform.KWAI_APP（快手主站），KwaiPlatform.Platform.NEBULA_APP（快手极速版）
        // 按数组顺序检查应用安装和版本情况，从中选择满足条件的第一个应用调起，若不设置则默认启动快手主站应用
        req.setPlatformArray(new String[] {KwaiPlatform.Platform.KWAI_APP, KwaiPlatform.Platform.NEBULA_APP});

        req.mediaInfo = new PostShareMediaInfo();

//        req.mediaInfo.mTag = mTagList.getText().toString();


        VideoObject videoObject = parseKsVideo(call, req);
        req.mediaInfo.mMultiMediaAssets = videoObject.mVideoPaths;
//        目前第三方app不可下方信息
//        req.mediaInfo.mTag = "111,222";
//        req.mediaInfo.mExtraInfo = "mExtraInfo";
//        req.mediaInfo.mM2uExtraInfo = "mM2uExtraInfo";

        // 设置不接受fallback
        // req.mediaInfo.mDisableFallback = false;
        // 输入透传的额外参数extraInfo
        // req.mediaInfo.mExtraInfo
        // 第三方埋点数据额外参数thirdExtraInfo
        // req.thirdExtraInfo
        // 业务参数mediaInfoMap（传入格式key1:value1;key2:value2）

        try {
            mKwaiOpenAPI.sendReq(req, activity);
        } catch (Exception e) {}

        result.success(null);
    }

      private void handleShareImageCallKs(MethodCall call, MethodChannel.Result result) {
        if (mKwaiOpenAPI == null) return;

        Log.i("SHARE----KS//", "handleShareImageCallKs");
        SinglePicturePublish.Req req = new SinglePicturePublish.Req();
        req.sessionId = mKwaiOpenAPI.getOpenAPISessionId();
        req.transaction = "SinglePicturePublish";
        // 设置功能调起快手支持应用，KwaiPlatform.Platform.KWAI_APP（快手主站），KwaiPlatform.Platform.NEBULA_APP（快手极速版）
        // 按数组顺序检查应用安装和版本情况，从中选择满足条件的第一个应用调起，若不设置则默认启动快手主站应用
        req.setPlatformArray(new String[] {KwaiPlatform.Platform.KWAI_APP, KwaiPlatform.Platform.NEBULA_APP});

        req.mediaInfo = new PostShareMediaInfo();

        ImageObject imageObject = parseKsImage(call, req);
        req.mediaInfo.mMultiMediaAssets = imageObject.mImagePaths;

//        目前第三方app不可下方信息
//        req.mediaInfo.mTag = "111";
//        req.mediaInfo.mExtraInfo = "mExtraInfo";
//        req.mediaInfo.mM2uExtraInfo = "mM2uExtraInfo";

        // 设置不接受fallback
        // req.mediaInfo.mDisableFallback = false;
        // 输入透传的额外参数extraInfo
        // req.mediaInfo.mExtraInfo
        // 第三方埋点数据额外参数thirdExtraInfo
        // req.thirdExtraInfo
        // 业务参数mediaInfoMap（传入格式key1:value1;key2:value2）

        try {
            mKwaiOpenAPI.sendReq(req, activity);
        } catch (Exception e) {}

        result.success(null);
    }
    // --- 快手 End


    private void registerApp(MethodCall call, MethodChannel.Result result) {
        final String clientKey = call.argument("client_key");
        DouYinOpenApiFactory.init(new DouYinOpenConfig(clientKey));
        result.success(null);
    }

    /// 授权
    private void handleAuthCall(MethodCall call, MethodChannel.Result result) {
        Authorization.Request request = new Authorization.Request();
        request.scope = call.argument("scope");
        request.state = call.argument("state");
        DouYinOpenApi openApi = createOpenApi();
        if (openApi != null) {
            openApi.authorize(request);
        }
        result.success(null);
    }

    private void handleShareCall(MethodCall call, MethodChannel.Result result) {
         Log.i("test","test 2");
        Share.Request request = new Share.Request();
        request.mState = call.argument("state");

        if ("shareImage".equals(call.method)) {
            Log.i("test","test 3");
            MediaContent mediaContent = new MediaContent();
            mediaContent.mMediaObject = parseImage(call);
            request.mMediaContent = mediaContent;

            request.mHashTagList = call.argument("hash_tags");
            ShareParam shareParam = new ShareParam();
            request.shareParam = shareParam;
            TitleObject titleObject = new TitleObject();
            shareParam.titleObject = titleObject;
            // titleObject.shortTitle  = "分享短标题";   // 抖音30.0.0版本开始支持该字段
            titleObject.title = call.argument("title");
            
        } else if ("shareVideo".equals(call.method)) {
            MediaContent mediaContent = new MediaContent();
            mediaContent.mMediaObject = parseVideo(call);
            request.mMediaContent = mediaContent;

            request.mHashTagList = call.argument("hash_tags");
            ShareParam shareParam = new ShareParam();
            request.shareParam = shareParam;
            TitleObject titleObject = new TitleObject();
            shareParam.titleObject = titleObject;
            // titleObject.shortTitle  = "分享短标题";   // 抖音30.0.0版本开始支持该字段
            titleObject.title = call.argument("title");
        } else if ("shareMicroApp".equals(call.method)) {
            request.mMicroAppInfo = parseMicroApp(call);
        } else if ("shareHashTags".equals(call.method)) {
            request.mHashTagList = call.argument("hash_tags");
        } else if ("shareAnchor".equals(call.method)) {
            request.mAnchorInfo = parseAnchor(call);
        }
        DouYinOpenApi openApi = createOpenApi();
        if (openApi != null) {
            openApi.share(request);
        }
        result.success(null);
    }



    private VideoObject parseKsVideo(MethodCall call, SingleVideoPublish.Req req) {
        VideoObject video = new VideoObject();
        ArrayList<String> videoPaths = new ArrayList<>();
        List<String> videoUris = call.argument("video_uris");
        for (String videoUri : videoUris) {
            String uri = KwaiFileProviderUtil.generateFileUriPath(activity, new File(Uri.parse(videoUri).getPath()), req, mKwaiOpenAPI);
            Log.i("SHARE----KS//", uri);
            videoPaths.add(uri);
        }

        video.mVideoPaths = videoPaths;
        return video;
    }

    private ImageObject parseKsImage(MethodCall call, SinglePicturePublish.Req req) {
        ImageObject image = new ImageObject();
        ArrayList<String> imagePaths = new ArrayList<>();
        List<String> imageUris = call.argument("image_uris");
        for (String imageUri : imageUris) {
            String uri = KwaiFileProviderUtil.generateFileUriPath(activity, new File(Uri.parse(imageUri).getPath()), req, mKwaiOpenAPI);
            Log.i("SHARE----KS//", uri);
            imagePaths.add(uri);
        }

        image.mImagePaths = imagePaths;
        return image;
    }

    private ImageObject parseImage(MethodCall call) {
        ImageObject image = new ImageObject();
        ArrayList<String> imagePaths = new ArrayList<>();
        List<String> imageUris = call.argument("image_uris");
        for (String imageUri : imageUris) {
            imagePaths.add(getShareFilePath(imageUri));
            Log.i("test 4", imageUri);
            // File file = new File("xxx文件路径");
            // //此处需要申明FileProvider，详情参考 Android 分享支持 FileProvider 的方式
            // Uri uri = FileProvider.getUriForFile(context, context.getPackageName() + ".fileProvide", file);
        }
        image.mImagePaths = imagePaths;
        return image;
    }

    private VideoObject parseVideo(MethodCall call) {
        VideoObject video = new VideoObject();
        ArrayList<String> videoPaths = new ArrayList<>();
        List<String> videoUris = call.argument("video_uris");
        for (String videoUri : videoUris) {

            String uri = getShareFilePath(videoUri);
            Log.i("SHARE----DY//", uri);

            videoPaths.add(uri);
        }
        video.mVideoPaths = videoPaths;
        return video;
    }

    private MicroAppInfo parseMicroApp(MethodCall call) {
        MicroAppInfo microApp = new MicroAppInfo();
        microApp.setAppId((String) call.argument("id"));
        microApp.setAppTitle((String) call.argument("title"));
        microApp.setAppUrl((String) call.argument("url"));
        microApp.setDescription((String) call.argument("description"));
        return microApp;
    }

    private AnchorObject parseAnchor(MethodCall call) {
        AnchorObject anchor = new AnchorObject();
        anchor.setAnchorTitle((String) call.argument("title"));
        anchor.setAnchorBusinessType((Integer) call.argument("business_type"));
        anchor.setAnchorContent((String) call.argument("content"));
        return anchor;
    }

    private void handleShareToContactsCall(MethodCall call, MethodChannel.Result result) {
        ShareToContact.Request request = new ShareToContact.Request();
        request.mState = call.argument("state");
        if ("shareImageToContacts".equals(call.method)) {
            MediaContent mediaContent = new MediaContent();
            mediaContent.mMediaObject = parseImage(call);
            request.mMediaContent = mediaContent;
        } else if ("shareHtmlToContacts".equals(call.method)) {
            request.htmlObject = parseHtml(call);
        }
        DouYinOpenApi openApi = createOpenApi();
        if (openApi != null) {
            openApi.shareToContacts(request);
        }
        result.success(null);
    }

    private ContactHtmlObject parseHtml(MethodCall call) {
        ContactHtmlObject html = new ContactHtmlObject();
        html.setTitle((String) call.argument("title"));
        html.setThumbUrl((String) call.argument("thumb_url"));
        html.setHtml((String) call.argument("url"));
        html.setDiscription((String) call.argument("discription"));
        return html;
    }

    private void handleOpenRecordCall(MethodCall call, MethodChannel.Result result) {
        OpenRecord.Request request = new OpenRecord.Request();
        request.mState = call.argument("state");
        DouYinOpenApi openApi = createOpenApi();
        if (openApi != null) {
            openApi.openRecordPage(request);
        }
        result.success(null);
    }

    // --- ViewDestroyListener

    @Override
    public boolean onViewDestroy(FlutterNativeView view) {
        if (register.compareAndSet(true, false)) {
            DouyinReceiver.unregisterReceiver(applicationContext, douyinReceiver);
        }
        return false;
    }

    // ---
    private String getShareFilePath(String fileUri) {
        DouYinOpenApi openApi = createOpenApi();
        if (openApi != null && openApi.isShareSupportFileProvider()) {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
                try {
                    ProviderInfo providerInfo = applicationContext.getPackageManager().getProviderInfo(new ComponentName(applicationContext, DouyinFileProvider.class), PackageManager.MATCH_DEFAULT_ONLY);
                    Uri shareFileUri = FileProvider.getUriForFile(applicationContext, providerInfo.authority, new File(Uri.parse(fileUri).getPath()));
                    applicationContext.grantUriPermission("com.ss.android.ugc.aweme", shareFileUri, Intent.FLAG_GRANT_READ_URI_PERMISSION);
                    return shareFileUri.toString();
                } catch (PackageManager.NameNotFoundException e) {
                    // ignore
                }
            }
        }
        return Uri.parse(fileUri).getPath();
    }
}
