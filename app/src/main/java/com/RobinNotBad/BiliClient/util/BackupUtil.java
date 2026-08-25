package com.RobinNotBad.BiliClient.util;

import android.content.SharedPreferences;

import com.RobinNotBad.BiliClient.BuildConfig;

import java.io.BufferedReader;
import java.io.StringReader;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * 设置与教程进度备份/恢复工具。
 * 将 SharedPreferences 中的设置备份到 /Documents/BiliClient/setting.txt（按设置树排序，含默认值），
 * 将教程进度备份到 /Documents/BiliClient/guide.txt。
 * 仅由实验室的"备份/恢复"开关、"备份"与"加载"按钮调用，无自动定时任务。
 * 文本格式：每行 "key=值类型:值"，保证顺序稳定且易读。
 */
public class BackupUtil {

    // 不参与备份的登录/会话 key
    private static final List<String> EXCLUDE_KEYS = new ArrayList<String>() {{
        add(SharedPreferencesUtil.cookies);
        add(SharedPreferencesUtil.csrf);
        add(SharedPreferencesUtil.mid);
        add(SharedPreferencesUtil.refresh_token);
        add(SharedPreferencesUtil.access_key);
        add(SharedPreferencesUtil.cookie_refresh);
        add("backup_restore_enable"); // 备份/恢复开关本身不备份，避免恢复时覆盖开关状态
        // 运行态/临时数据，不应备份
        add("wbi_mixin_key");
        add("last_wbi");
        add("dynamic_update_baseline");
        add("dynamic_update_num");
        add("message_update_num");
        add("terminal_update_pkg");
        add("app_version_last");
        add("app_version_check");
        add("app_announcement_last");
        add("dev_test_link");
        add("dev_catgirl_apikey");
        add(SharedPreferencesUtil.search_history); // 搜索历史为个人数据，由"数据迁移"单独导出/合并
    }};

    // 按设置树顺序排列的设置项，值为默认值（value 的 Java 类型决定存储类型）
    // 备份时：若 SharedPreferences 有值用当前值，否则用默认值 → 保证所有设置项都被备份
    private static final Map<String, Object> SETTINGS = new LinkedHashMap<>();

    static {
        // 设置主页
        SETTINGS.put("auto_check_update_enable", Boolean.TRUE);
        // 选择播放器
        SETTINGS.put("player", "null");
        SETTINGS.put("play_qn", 16);
        // 内置播放器
        SETTINGS.put("player_longclick", Boolean.TRUE);
        SETTINGS.put("player_loop", Boolean.FALSE);
        SETTINGS.put("player_background", Boolean.FALSE);
        SETTINGS.put("player_autolandscape", Boolean.FALSE);
        SETTINGS.put("player_from_last", Boolean.TRUE);
        SETTINGS.put("player_show_online", Boolean.FALSE);
        SETTINGS.put("player_audio_only", Boolean.FALSE);
        SETTINGS.put("player_scale", Boolean.TRUE);
        SETTINGS.put("player_doublemove", Boolean.TRUE);
        SETTINGS.put("player_display", Boolean.TRUE);
        SETTINGS.put("player_codec", Boolean.TRUE);
        SETTINGS.put("player_audio", Boolean.FALSE);
        SETTINGS.put("player_high_energy", Boolean.FALSE);
        SETTINGS.put("player_danmaku_allowoverlap", Boolean.TRUE);
        SETTINGS.put("player_danmaku_mergeduplicate", Boolean.FALSE);
        SETTINGS.put("player_danmaku_forceR2L", Boolean.FALSE);
        SETTINGS.put("player_danmaku_showsender", Boolean.TRUE);
        SETTINGS.put("player_danmaku_maxline", 10);
        SETTINGS.put("player_danmaku_size", 0.7f);
        SETTINGS.put("player_danmaku_transparency", 0.7f);
        SETTINGS.put("player_danmaku_speed", 1.0f);
        SETTINGS.put("player_subtitle_autoshow", Boolean.TRUE);
        SETTINGS.put("player_subtitle_ai_allowed", Boolean.FALSE);
        SETTINGS.put("player_subtitle_delta", 0.0f);
        SETTINGS.put("player_ui_showRotateBtn", Boolean.TRUE);
        SETTINGS.put("player_ui_showDanmakuBtn", Boolean.TRUE);
        SETTINGS.put("player_ui_showQualityBtn", Boolean.TRUE);
        SETTINGS.put("player_ui_showPageBtn", Boolean.TRUE);
        SETTINGS.put("pref_switch_danmaku", Boolean.TRUE);
        // 界面设置
        SETTINGS.put("dpi", 1.0f);
        SETTINGS.put("density", -1);
        SETTINGS.put("paddingH_percent", 0);
        SETTINGS.put("paddingV_percent", 0);
        SETTINGS.put("player_ui_round", Boolean.FALSE);
        // 菜单设置
        SETTINGS.put("menu_popular", Boolean.TRUE);
        SETTINGS.put("menu_live", Boolean.FALSE);
        SETTINGS.put("menu_precious", Boolean.FALSE);
        SETTINGS.put("menu_sort", "");
        // 偏好设置-功能
        SETTINGS.put("copy_enable", Boolean.TRUE);
        SETTINGS.put("creative_enable", Boolean.TRUE);
        SETTINGS.put("search_suggestions_enable", Boolean.TRUE);
        SETTINGS.put(SharedPreferencesUtil.SEARCH_DEFAULT_CONTENT_ENABLE, Boolean.FALSE);
        SETTINGS.put("link_enable", Boolean.TRUE);
        SETTINGS.put(SharedPreferencesUtil.DYNAMIC_UPDATE_CHECK_ENABLE, Boolean.TRUE);
        SETTINGS.put(SharedPreferencesUtil.MESSAGE_UPDATE_CHECK_ENABLE, Boolean.TRUE);
        SETTINGS.put(SharedPreferencesUtil.PRIVATE_MSG_AUTO_READ_ENABLE, Boolean.TRUE);
        SETTINGS.put(SharedPreferencesUtil.FOLLOW_GROUP_MODE, Boolean.FALSE);
        SETTINGS.put(SharedPreferencesUtil.NIGHT_REMINDER_ENABLE, Boolean.TRUE);
        // 偏好设置-优化
        SETTINGS.put("back_disable", Boolean.FALSE);
        SETTINGS.put("save_ban_gallery", Boolean.TRUE);
        SETTINGS.put("image_request_jpg", Boolean.FALSE);
        // 偏好设置-视觉
        SETTINGS.put(SharedPreferencesUtil.LOAD_TRANSITION, Boolean.TRUE);
        SETTINGS.put("image_no_load_onscroll", Boolean.FALSE);
        SETTINGS.put(SharedPreferencesUtil.ASYNC_INFLATE_ENABLE, Boolean.TRUE);
        SETTINGS.put(SharedPreferencesUtil.SNACKBAR_ENABLE, Boolean.TRUE);
        // 偏好设置-表冠
        SETTINGS.put("ui_rotatory_enable", Boolean.FALSE);
        SETTINGS.put("ui_rotatory_recycler", 0.0f);
        SETTINGS.put("ui_rotatory_scroll", 0.0f);
        // 评论区
        SETTINGS.put(SharedPreferencesUtil.NO_VIP_COLOR, Boolean.FALSE);
        SETTINGS.put(SharedPreferencesUtil.NO_MEDAL, Boolean.FALSE);
        SETTINGS.put(SharedPreferencesUtil.REPLY_MARQUEE_NAME, Boolean.TRUE);
        // 详情页
        SETTINGS.put("fav_single", Boolean.FALSE);
        SETTINGS.put("fav_notice", Boolean.TRUE);
        SETTINGS.put("cover_play_enable", Boolean.TRUE);
        SETTINGS.put("tags_enable", Boolean.TRUE);
        SETTINGS.put("related_enable", Boolean.TRUE);
        SETTINGS.put("live_by_guest", Boolean.FALSE);
        SETTINGS.put("like_one_triple", Boolean.TRUE);
        // 实验室
        SETTINGS.put("new_danmaku_api", Boolean.TRUE);
        SETTINGS.put(SharedPreferencesUtil.PRIVATE_MSG_UNREAD_BADGE_ENABLE, Boolean.FALSE);
        SETTINGS.put("dev_download_old", Boolean.FALSE);
        SETTINGS.put("save_path_video", "");
        SETTINGS.put("save_path_pictures", "");
        SETTINGS.put("ui_landscape", Boolean.FALSE);
        SETTINGS.put("ui_splashtext", "欢迎使用\n哔哩终端");
        SETTINGS.put("marquee_enable", Boolean.TRUE);
        SETTINGS.put("dev_player_rotate_software", Boolean.FALSE);
        SETTINGS.put("player_show_viewpoints", Boolean.FALSE);
        SETTINGS.put(SharedPreferencesUtil.PLAYER_MEDIA_SESSION_ENABLE, Boolean.FALSE);
        SETTINGS.put("player_interaction_debug", Boolean.FALSE);
        SETTINGS.put("dev_logv", Boolean.FALSE);
        SETTINGS.put("dev_logd", Boolean.FALSE);
        SETTINGS.put("dev_logi", Boolean.FALSE);
        SETTINGS.put("dev_jsonerr_detailed", Boolean.FALSE);
        SETTINGS.put("dev_recyclererr_detailed", Boolean.FALSE);
        // 其他用户配置
        SETTINGS.put("developer", Boolean.FALSE);
        SETTINGS.put("first_play", Boolean.TRUE);
        SETTINGS.put("first_videoinfo", Boolean.TRUE);
        SETTINGS.put("first_LoginActivity", Boolean.TRUE);
        SETTINGS.put("disclaimer_shown", Boolean.FALSE);
        SETTINGS.put("setup", Boolean.FALSE);
    }

    private BackupUtil() {
    }

    private static boolean isExcluded(String key) {
        return EXCLUDE_KEYS.contains(key);
    }

    /** 生成备份文件头部的说明信息（保存日期、客户端版本），以 # 开头便于区分 */
    private static String buildHeader(String typeName) {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault());
        String date = sdf.format(new Date());
        String version = "unknown";
        try {
            version = BuildConfig.VERSION_NAME;
        } catch (Exception ignored) {
        }
        return "# " + typeName + "\n"
                + "# 保存日期: " + date + "\n"
                + "# 客户端版本: " + version + "\n";
    }

    /**
     * 备份所有设置到 setting.txt（按设置树排序，含默认值），返回是否成功。
     */
    public static boolean backupSettings() {
        try {
            SharedPreferences prefs = SharedPreferencesUtil.getSharedPreferences();
            Map<String, ?> all = prefs.getAll();
            StringBuilder sb = new StringBuilder();
            sb.append(buildHeader("哔哩终端 设置备份"));
            // 按设置树顺序，对所有设置项输出（有值用当前值，无值用默认值）
            for (Map.Entry<String, Object> entry : SETTINGS.entrySet()) {
                String key = entry.getKey();
                Object def = entry.getValue();
                if (isExcluded(key)) continue;
                Object value = all.containsKey(key) ? all.get(key) : def;
                // 路径类设置：未设置时使用实际默认路径，避免恢复后为空
                if (value instanceof String && ((String) value).isEmpty()) {
                    if ("save_path_video".equals(key)) value = FileUtil.getVideoDownloadPath().toString();
                    else if ("save_path_pictures".equals(key)) value = FileUtil.getPicturePath().toString();
                }
                appendLine(sb, key, value);
            }
            // 再输出不在列表中的其他 key（用户新增或遗漏的），按字典序
            List<String> rest = new ArrayList<>();
            for (String key : all.keySet()) {
                if (!SETTINGS.containsKey(key) && !isExcluded(key)) rest.add(key);
            }
            java.util.Collections.sort(rest);
            for (String key : rest) {
                appendLine(sb, key, all.get(key));
            }
            return FileUtil.writeString(FileUtil.getSettingFile(), sb.toString());
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    private static void appendLine(StringBuilder sb, String key, Object value) {
        String val;
        if (value instanceof String) val = "s:" + escape((String) value);
        else if (value instanceof Boolean) val = "b:" + value;
        else if (value instanceof Integer) val = "i:" + value;
        else if (value instanceof Long) val = "l:" + value;
        else if (value instanceof Float) val = "f:" + value;
        else return; // 不支持的类型跳过
        sb.append(key).append('=').append(val).append('\n');
    }

    // 对值做转义：\\ 和 \n 反转义，避免破坏行结构
    private static String escape(String s) {
        return s.replace("\\", "\\\\").replace("\n", "\\n").replace("=", "\\=");
    }

    private static String unescape(String s) {
        return s.replace("\\=", "=").replace("\\n", "\n").replace("\\\\", "\\");
    }

    /**
     * 从 setting.txt 恢复设置，返回是否成功。
     */
    public static boolean restoreSettings() {
        try {
            String content = FileUtil.readString(FileUtil.getSettingFile());
            if (content == null || content.isEmpty()) return true; // 无可恢复内容，不算失败
            SharedPreferences.Editor editor = SharedPreferencesUtil.getSharedPreferences().edit();
            BufferedReader reader = new BufferedReader(new StringReader(content));
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.isEmpty() || line.startsWith("#")) continue; // 跳过空行和注释头
                int eq = line.indexOf('=');
                if (eq <= 0) continue;
                String key = line.substring(0, eq);
                if (isExcluded(key)) continue;
                String typed = line.substring(eq + 1);
                applyValue(editor, key, typed);
            }
            editor.apply();
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    private static void applyValue(SharedPreferences.Editor editor, String key, String typed) {
        if (typed.length() < 3) return; // 至少 "x:" + 值
        char type = typed.charAt(0);
        // 跳过类型字符和冒号，如 "s:/storage" -> "/storage"
        String val = typed.substring(2);
        try {
            switch (type) {
                case 's':
                    editor.putString(key, unescape(val));
                    break;
                case 'b':
                    editor.putBoolean(key, "true".equals(val));
                    break;
                case 'i':
                    editor.putInt(key, Integer.parseInt(val));
                    break;
                case 'l':
                    editor.putLong(key, Long.parseLong(val));
                    break;
                case 'f':
                    editor.putFloat(key, Float.parseFloat(val));
                    break;
            }
        } catch (Exception ignored) {
        }
    }

    /**
     * 备份教程进度到 guide.txt，返回是否成功。
     */
    public static boolean backupTutorial() {
        try {
            Map<String, ?> all = SharedPreferencesUtil.getSharedPreferences().getAll();
            StringBuilder sb = new StringBuilder();
            sb.append(buildHeader("哔哩终端 教程进度备份"));
            List<String> keys = new ArrayList<>();
            for (String key : all.keySet()) {
                if (key.startsWith("tutorial_ver_") || key.startsWith("tutorial_pager_")) keys.add(key);
            }
            java.util.Collections.sort(keys);
            for (String key : keys) {
                appendLine(sb, key, all.get(key));
            }
            return FileUtil.writeString(FileUtil.getGuideFile(), sb.toString());
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * 从 guide.txt 恢复教程进度，返回是否成功。
     * 空文件视为"无教程进度可恢复"，不算失败。
     */
    public static boolean restoreTutorial() {
        try {
            String content = FileUtil.readString(FileUtil.getGuideFile());
            if (content == null || content.isEmpty()) return true; // 无内容可恢复，不算失败
            SharedPreferences.Editor editor = SharedPreferencesUtil.getSharedPreferences().edit();
            BufferedReader reader = new BufferedReader(new StringReader(content));
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.isEmpty() || line.startsWith("#")) continue; // 跳过空行和注释头
                int eq = line.indexOf('=');
                if (eq <= 0) continue;
                String key = line.substring(0, eq);
                String typed = line.substring(eq + 1);
                applyValue(editor, key, typed);
            }
            editor.apply();
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * 仅执行备份（设置 + 教程），返回是否成功。
     */
    public static boolean backupOnly() {
        boolean s = backupSettings();
        boolean g = backupTutorial();
        return s && g;
    }

    /**
     * 仅执行恢复（设置 + 教程），返回是否成功。
     * 空文件视为无可恢复内容，不算失败。
     */
    public static boolean restoreOnly() {
        boolean s = restoreSettings();
        boolean g = restoreTutorial();
        return s && g;
    }
}
