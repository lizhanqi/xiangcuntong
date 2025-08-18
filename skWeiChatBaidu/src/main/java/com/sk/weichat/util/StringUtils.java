package com.sk.weichat.util;

import android.content.Context;
import android.text.Html;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.TextUtils;
import android.text.style.ForegroundColorSpan;

import com.sk.weichat.MyApplication;
import com.sk.weichat.R;
import com.sk.weichat.bean.message.ChatMessage;
import com.sk.weichat.bean.message.XmppMessage;
import com.sk.weichat.ui.base.CoreManager;

import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.sk.weichat.bean.message.XmppMessage.TYPE_CHAT_HISTORY;
import static com.sk.weichat.bean.message.XmppMessage.TYPE_IMAGE_TEXT;
import static com.sk.weichat.bean.message.XmppMessage.TYPE_IMAGE_TEXT_MANY;
import static com.sk.weichat.bean.message.XmppMessage.TYPE_REPLAY;

public class StringUtils {
    private final static Pattern email_pattern = Pattern.compile("\\w+([-+.]\\w+)*@\\w+([-.]\\w+)*\\.\\w+([-.]\\w+)*");
    private static final String SEP1 = "#";
    private static final String SEP2 = "|";
    private static final String SEP3 = "=";
    static Pattern phoneNumberPat = Pattern.compile("^((13[0-9])|(147)|(15[0-3,5-9])|(17[0,6-8])|(18[0-9]))\\d{8}$");
    static Pattern nickNamePat = Pattern.compile("^[\u4e00-\u9fa5_a-zA-Z0-9_]{3,15}$");// 3-10个字符
    static Pattern searchNickNamePat = Pattern.compile("^[\u4e00-\u9fa5_a-zA-Z0-9_]*$");// 不限制长度，可以为空字符串
    static Pattern companyNamePat = Pattern.compile("^[\u4e00-\u9fa5_a-zA-Z0-9_]{3,50}$");// 3-50个字符

    /**
     * EditText显示Error
     *
     * @param context
     * @param resId
     * @return
     */
    public static CharSequence editTextHtmlErrorTip(Context context, int resId) {
        CharSequence html = Html.fromHtml("<font color='red'>" + context.getString(resId) + "</font>");
        return html;
    }

    public static CharSequence editTextHtmlErrorTip(Context context, String text) {
        CharSequence html = Html.fromHtml("<font color='red'>" + text + "</font>");
        return html;
    }

    /* 是否是手机号 */
    public static boolean isMobileNumber(String mobiles) {
        // 正则匹配不行，新号段和国外都不行，
        return true;
/*
        Matcher mat = phoneNumberPat.matcher(mobiles);
        return mat.matches();
*/
    }

    /* 检测之前，最好检测是否为空。检测是否是正确的昵称格式 */
    public static boolean isNickName(String nickName) {
        if (TextUtils.isEmpty(nickName)) {
            return false;
        }
        Matcher mat = nickNamePat.matcher(nickName);
        return mat.matches();
    }

    public static boolean isSearchNickName(String nickName) {
        if (nickName == null) {// 防止异常
            return false;
        }
        Matcher mat = searchNickNamePat.matcher(nickName);
        return mat.matches();
    }

    public static boolean isCompanyName(String name) {
        if (TextUtils.isEmpty(name)) {
            return false;
        }
        Matcher mat = companyNamePat.matcher(name);
        return mat.matches();
    }

    public static SpannableString matcherSearchTitle(int color, String text, String keyword) {
        if (TextUtils.isEmpty(text)) {
            text = "null";
        }
        String str = text.toLowerCase();
        String key = "";
        if (!TextUtils.isEmpty(keyword)) {
            key = keyword.toLowerCase();
        }
        Pattern pattern = Pattern.compile(key);
        Matcher matcher = pattern.matcher(str);
        SpannableString spannableString = new SpannableString(text);
        while (matcher.find()) {
            int start = matcher.start();
            int end = matcher.end();
            spannableString.setSpan(new ForegroundColorSpan(color), start, end,
                    Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        }
        return spannableString;
    }

    /**
     * 判断是不是一个合法的电子邮件地址
     *
     * @param email
     * @return
     */
    public static boolean isEmail(String email) {
        if (email == null || email.trim().length() == 0)
            return false;
        return email_pattern.matcher(email).matches();
    }

    public static boolean strEquals(String s1, String s2) {
        if (s1 == s2) {// 引用相等直接返回true
            return true;
        }
        boolean emptyS1 = s1 == null || s1.trim().length() == 0;
        boolean emptyS2 = s2 == null || s2.trim().length() == 0;
        if (emptyS1 && emptyS2) {// 都为空，认为相等
            return true;
        }
        if (s1 != null) {
            return s1.equals(s2);
        }
        if (s2 != null) {
            return s2.equals(s1);
        }
        return false;
    }

    /**
     * 去掉特殊字符
     * 这个方法开发原因不明，但\r会导致ios显示问题，所以去掉了这个方法，
     */
    public static String replaceSpecialChar(String str) {
        if (str != null && str.length() > 0) {
            return str;
        }
        return "";
    }

    public static String ListToString(List<?> list) {
        StringBuffer sb = new StringBuffer();
        if (list != null && list.size() > 0) {
            for (int i = 0; i < list.size(); i++) {
                if (list.get(i) == null || list.get(i) == "") {
                    continue;
                }
                // 如果值是list类型则调用自己
                if (list.get(i) instanceof List) {
                    sb.append(ListToString((List<?>) list.get(i)));
                    sb.append(SEP1);
                } else if (list.get(i) instanceof Map) {
                    sb.append(MapToString((Map<?, ?>) list.get(i)));
                    sb.append(SEP1);
                } else {
                    sb.append(list.get(i));
                    sb.append(SEP1);
                }
            }
        }
        return "L" + sb.toString();
    }

    public static String MapToString(Map<?, ?> map) {
        StringBuffer sb = new StringBuffer();
        // 遍历map
        for (Object obj : map.keySet()) {
            if (obj == null) {
                continue;
            }
            Object key = obj;
            Object value = map.get(key);
            if (value instanceof List<?>) {
                sb.append(key.toString() + SEP1 + ListToString((List<?>) value));
                sb.append(SEP2);
            } else if (value instanceof Map<?, ?>) {
                sb.append(key.toString() + SEP1
                        + MapToString((Map<?, ?>) value));
                sb.append(SEP2);
            } else {
                sb.append(key.toString() + SEP3 + value.toString());
                sb.append(SEP2);
            }
        }
        return "M" + sb.toString();
    }

    public static List<Object> StringToList(String listText) {
        if (listText == null || listText.equals("")) {
            return null;
        }
        listText = listText.substring(1);

        listText = listText;

        List<Object> list = new ArrayList<Object>();
        String[] text = listText.split(SEP1);
        for (String str : text) {
            if (str.charAt(0) == 'M') {
                Map<?, ?> map = StringToMap(str);
                list.add(map);
            } else if (str.charAt(0) == 'L') {
                List<?> lists = StringToList(str);
                list.add(lists);
            } else {
                list.add(str);
            }
        }
        return list;
    }

    public static Map<String, Object> StringToMap(String mapText) {

        if (mapText == null || mapText.equals("")) {
            return null;
        }
        mapText = mapText.substring(1);

        mapText = mapText;

        Map<String, Object> map = new HashMap<String, Object>();
        String[] text = mapText.split("\\" + SEP2); // 转换为数组
        for (String str : text) {
            String[] keyText = str.split(SEP3); // 转换key与value的数组
            if (keyText.length < 1) {
                continue;
            }
            String key = keyText[0]; // key
            String value = keyText[1]; // value
            if (value.charAt(0) == 'M') {
                Map<?, ?> map1 = StringToMap(value);
                map.put(key, map1);
            } else if (value.charAt(0) == 'L') {
                List<?> list = StringToList(value);
                map.put(key, list);
            } else {
                map.put(key, value);
            }
        }
        return map;
    }

    /**
     *      * 获取double数据小数点后两位不进行四舍五入
     *      * @param value
     *      * @return -1 , double
     *     
     */
    public static String getMoney(double value) {
        String cutString = "" + value;
        String[] strings = cutString.split("\\.");
        String point = strings[1].length() > 1 ? strings[1].substring(0, 2) : strings[1].substring(0, 1);
        return strings[0] + "." + point;
    }

    /**
     * 冒泡排序
     *
     * @param array
     * @return
     */
    public static int[] bubbleSort(int[] array) {
        if (array.length == 0)
            return array;
        for (int i = 0; i < array.length; i++)
            for (int j = 0; j < array.length - 1 - i; j++)
                if (array[j + 1] < array[j]) {
                    int temp = array[j + 1];
                    array[j + 1] = array[j];
                    array[j] = temp;
                }
        return array;
    }

    public static String add(String v1, String v2) {
        BigDecimal b1 = new BigDecimal(v1);
        BigDecimal b2 = new BigDecimal(v2);
        return b1.add(b2).setScale(2, BigDecimal.ROUND_HALF_UP).toString();
    }

    /**
     * 根据消息类型返回对应的文字
     *
     * @param chatMessage
     * @param isShowName：是否将发送方名字拼接返回
     * @return : 返回type对应的内容
     * <p>
     * todo 发现多个地方都封装了类似的方法来处理这个事情，这样每次新增消息类型，这些地方都需要处理一下，看起来很乱也很麻烦，
     * todo 现先统一几个地方的使用，剩下的三个地方(下面三个todo)暂时不改动
     * todo ChatMessage: getSimpleContent(use for MessageFragment relay / Relay message type)
     * todo FriendDao: updateFriendContent、updateApartDownloadTime(use for update friend content for show)
     * todo CoreService: notificationMessage(user for local notify)
     * <p>
     * 目前使用的地方：
     * 1.TalkHistoryActivity(回话记录Activity)
     * 2.ChatHistoryHolder("聊天记录"Holder)
     * 3.ChatHistoryActivity("聊天记录"Activity)
     * <p>
     */
    public static String getMessageContent(ChatMessage chatMessage, boolean isShowName) {
        int type = chatMessage.getType();
        String context = "";
        if (type == XmppMessage.TYPE_TEXT) {
            context = chatMessage.getContent();
            if (chatMessage.getIsReadDel() && !chatMessage.isMySend()) {
                context = "[" + MyApplication.getInstance().getString(R.string.delete_after_read) + MyApplication.getInstance().getString(R.string.message) + "]";
            }
        } else if (type == XmppMessage.TYPE_VOICE) {
            context = "[" + MyApplication.getInstance().getString(R.string.voice) + "]";
        } else if (type == XmppMessage.TYPE_GIF) {
            context = "[" + MyApplication.getInstance().getString(R.string.animation) + "]";
        } else if (type == XmppMessage.TYPE_IMAGE) {
            context = "[" + MyApplication.getInstance().getString(R.string.image) + "]";
        } else if (type == XmppMessage.TYPE_VIDEO) {
            context = "[" + MyApplication.getInstance().getString(R.string.s_video) + "]";
        } else if (type == XmppMessage.TYPE_FILE) {
            context = "[" + MyApplication.getInstance().getString(R.string.s_file) + "]";
        } else if (type == XmppMessage.TYPE_LOCATION) {
            context = "[" + MyApplication.getInstance().getString(R.string.my_location) + "]";
        } else if (type == XmppMessage.TYPE_CARD) {
            context = "[" + MyApplication.getInstance().getString(R.string.chat_card) + "]";
        } else if (type == XmppMessage.TYPE_RED) {
            context = "[" + MyApplication.getInstance().getString(R.string.chat_red) + "]";
        } else if (type == XmppMessage.TYPE_SHAKE) {
            context = MyApplication.getContext().getString(R.string.msg_shake);
        } else if (type == XmppMessage.TYPE_DICE) {
            context = MyApplication.getContext().getString(R.string.type_dice);
        } else if (type == XmppMessage.TYPE_RPS) {
            context = MyApplication.getContext().getString(R.string.type_rps);
        } else if (type == XmppMessage.TYPE_LINK || type == XmppMessage.TYPE_SHARE_LINK) {
            context = "[" + MyApplication.getInstance().getString(R.string.link) + "]";
        } else if (type == TYPE_IMAGE_TEXT || type == TYPE_IMAGE_TEXT_MANY) {
            context = "[" + MyApplication.getInstance().getString(R.string.graphic) + MyApplication.getInstance().getString(R.string.mainviewcontroller_message) + "]";
        } else if (type == TYPE_REPLAY) {
            context = "[" + MyApplication.getInstance().getString(R.string.replay) + "]";
        } else if (type == TYPE_CHAT_HISTORY) {
            context = MyApplication.getContext().getString(R.string.msg_chat_history);
        } else if (type == XmppMessage.TYPE_TRANSFER) {
            context = MyApplication.getContext().getString(R.string.tip_transfer_money);
        } else if (type == XmppMessage.TYPE_TRANSFER_RECEIVE) {
            context = MyApplication.getContext().getString(R.string.tip_transfer_money) + MyApplication.getContext().getString(R.string.transfer_friend_sure_save);
        } else if (type == XmppMessage.TYPE_TRANSFER_BACK) {
            context = MyApplication.getContext().getString(R.string.transfer_back);
        } else if (type == XmppMessage.TYPE_PAYMENT_OUT || type == XmppMessage.TYPE_RECEIPT_OUT) {
            context = MyApplication.getContext().getString(R.string.payment_get_notify);
        } else if (type == XmppMessage.TYPE_PAYMENT_GET || type == XmppMessage.TYPE_RECEIPT_GET) {
            context = MyApplication.getContext().getString(R.string.receipt_get_notify);
        } else if (type == XmppMessage.TYPE_SECURE_LOST_KEY) {
            context = MyApplication.getContext().getString(R.string.request_chat_key_group_thumb);
        } else if (type >= XmppMessage.TYPE_IS_CONNECT_VOICE
                && type <= XmppMessage.TYPE_EXIT_VOICE) {
            context = MyApplication.getContext().getString(R.string.msg_video_voice);
        } else if (type == XmppMessage.TYPE_TIP) {
            context = MyApplication.getContext().getString(R.string.system_message);
        }
        if (isShowName) {
            return chatMessage.getFromUserName() + "：" + context;
        } else {
            return context;
        }
    }

    /**
     * 音视频相关消息的content显示，因为国际化，各端发送的content不统一等问题，不能直接取消息内的content显示，
     * 只能本地根据type等关键字段单独处理，现专门封装一个方法处理这个问题
     */
    public static String getAudioMessageContent(ChatMessage chatMessage) {
        int type = chatMessage.getType();
        String mLoginUserId = CoreManager.requireSelf(MyApplication.getContext()).getUserId();
        String content = "";
        if (type == XmppMessage.TYPE_NO_CONNECT_VOICE
                || type == XmppMessage.TYPE_NO_CONNECT_VIDEO
                || type == XmppMessage.TYPE_NO_CONNECT_SCREEN) {
            if (chatMessage.getTimeLen() == 0) {
                if (type == XmppMessage.TYPE_NO_CONNECT_VOICE) {
                    content = MyApplication.getContext().getString(R.string.sip_canceled) + MyApplication.getContext().getString(R.string.voice_chat);
                } else if (type == XmppMessage.TYPE_NO_CONNECT_VIDEO) {
                    content = MyApplication.getContext().getString(R.string.sip_canceled) + MyApplication.getContext().getString(R.string.video_call);
                } else {
                    content = MyApplication.getContext().getString(R.string.sip_canceled) + MyApplication.getContext().getString(R.string.screen_call);
                }
            } else {
                if (TextUtils.equals(mLoginUserId, chatMessage.getFromUserId())) {
                    content = MyApplication.getContext().getString(R.string.sip_refused);
                } else {
                    content = MyApplication.getContext().getString(R.string.sip_remote_refused);
                }
            }
        } else if (type == XmppMessage.TYPE_END_CONNECT_VOICE
                || type == XmppMessage.TYPE_END_CONNECT_VIDEO
                || type == XmppMessage.TYPE_END_CONNECT_SCREEN) {
            int timeLen = chatMessage.getTimeLen();
            if (type == XmppMessage.TYPE_END_CONNECT_VOICE) {
                content = MyApplication.getContext().getString(R.string.finished) + MyApplication.getContext().getString(R.string.voice_chat) + ","
                        + MyApplication.getContext().getString(R.string.time_len) + getTimeLengthString(timeLen);
            } else if (type == XmppMessage.TYPE_END_CONNECT_VIDEO) {
                content = MyApplication.getContext().getString(R.string.finished) + MyApplication.getContext().getString(R.string.video_call) + ","
                        + MyApplication.getContext().getString(R.string.time_len) + getTimeLengthString(timeLen);
            } else {
                content = MyApplication.getContext().getString(R.string.finished) + MyApplication.getContext().getString(R.string.screen_call) + ","
                        + MyApplication.getContext().getString(R.string.time_len) + getTimeLengthString(timeLen);
            }
        } else if (type == XmppMessage.TYPE_IS_MU_CONNECT_VOICE) {
            content = MyApplication.getContext().getString(R.string.tip_invite_voice_meeting);
        } else if (type == XmppMessage.TYPE_IS_MU_CONNECT_VIDEO) {
            content = MyApplication.getContext().getString(R.string.tip_invite_video_meeting);
        } else if (type == XmppMessage.TYPE_IS_MU_CONNECT_SCREEN) {
            content = MyApplication.getContext().getString(R.string.tip_invite_screen_meeting);
        } else if (type == XmppMessage.TYPE_IS_MU_CONNECT_TALK) {
            content = MyApplication.getContext().getString(R.string.tip_invite_talk_meeting);
        } else if (type == XmppMessage.TYPE_IS_BUSY) {
            if (chatMessage.isMySend()) {
                content = MyApplication.getContext().getString(R.string.busy_he);
            } else {
                content = MyApplication.getContext().getString(R.string.busy_me);
            }
        }
        return content;
    }

    private static String getTimeLengthString(int timeLen) {
        SimpleDateFormat sdf;
        if (timeLen < TimeUnit.HOURS.toSeconds(1)) {
            sdf = new SimpleDateFormat("mm:ss");
        } else {
            sdf = new SimpleDateFormat("HH:mm:ss");
        }
        return sdf.format(new Date(TimeUnit.SECONDS.toMillis(timeLen)));
    }
}