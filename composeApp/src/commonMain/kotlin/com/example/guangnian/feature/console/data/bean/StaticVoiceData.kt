package com.example.guangnian.feature.console.data.bean

/**
 * KMP 共享的静态音色数据池
 * 整合了来自火山引擎 和 微软 Azure 的全量音色数据
 */
object StaticVoiceData {
    val allVoices: List<VoiceBean> by lazy {
        val list = mutableListOf<VoiceBean>()

        // 1. 火山引擎 (Volcano) 全量音色
        list.addAll(getVolcanoVoices())

        // 2. 讯飞 (AIUI) 全量音色
        list.addAll(getAiuiVoices())

        // 3. 微软 (Microsoft) 全量音色
        list.addAll(getMicrosoftVoices())

        list
    }

    private fun getVolcanoVoices(): List<VoiceBean> = listOf(
        VoiceBean("zh_female_wanqudashu_moon_bigtts", "中文", "湾区大叔", SourceType.VOLCANO),
        VoiceBean("zh_female_daimengchuanmei_moon_bigtts", "中文", "呆萌川妹", SourceType.VOLCANO),
        VoiceBean("zh_male_guozhoudege_moon_bigtts", "中文", "广州德哥", SourceType.VOLCANO),
        VoiceBean("zh_male_beijingxiaoye_moon_bigtts", "中文", "北京小爷", SourceType.VOLCANO),
        VoiceBean("zh_male_shaonianzixin_moon_bigtts", "中/英", "少年梓辛/Brayan", SourceType.VOLCANO),
        VoiceBean("zh_female_meilinvyou_moon_bigtts", "中文", "魅力女友", SourceType.VOLCANO),
        VoiceBean("zh_male_shenyeboke_moon_bigtts", "中文", "深夜播客", SourceType.VOLCANO),
        VoiceBean("zh_female_sajiaonvyou_moon_bigtts", "中文", "柔美女友", SourceType.VOLCANO),
        VoiceBean("zh_female_yuanqinvyou_moon_bigtts", "中文", "撒娇学妹", SourceType.VOLCANO),
        VoiceBean("zh_male_haoyuxiaoge_moon_bigtts", "中文", "浩宇小哥", SourceType.VOLCANO),
        VoiceBean("zh_male_guangxiyuanzhou_moon_bigtts", "中文", "广西远舟", SourceType.VOLCANO),
        VoiceBean("zh_female_meituojieer_moon_bigtts", "中文", "妹坨洁儿", SourceType.VOLCANO),
        VoiceBean("zh_male_yuzhouzixuan_moon_bigtts", "中文", "豫州子轩", SourceType.VOLCANO),
        VoiceBean("zh_female_linjianvhai_moon_bigtts", "中文", "邻家女孩", SourceType.VOLCANO),
        VoiceBean("zh_female_gaolengyujie_moon_bigtts", "中文", "高冷御姐", SourceType.VOLCANO),
        VoiceBean("zh_male_yuanboxiaoshu_moon_bigtts", "中文", "渊博小叔", SourceType.VOLCANO),
        VoiceBean("zh_male_yangguangqingnian_moon_bigtts", "中文", "阳光青年", SourceType.VOLCANO),
        VoiceBean("zh_male_aojiaobazong_moon_bigtts", "中文", "傲娇霸总", SourceType.VOLCANO),
        VoiceBean("zh_male_jingqiangkanye_moon_bigtts", "中/英", "京腔侃爷/Harmony", SourceType.VOLCANO),
        VoiceBean("zh_female_shuangkuaisisi_moon_bigtts", "中/英", "爽快思思/Skye", SourceType.VOLCANO),
        VoiceBean("zh_male_wennuanahu_moon_bigtts", "中/英", "温暖阿虎/Alvin", SourceType.VOLCANO),
        VoiceBean("zh_female_wanwanxiaohe_moon_bigtts", "中文", "湾湾小何", SourceType.VOLCANO),
        VoiceBean("multi_female_shuangkuaisisi_moon_bigtts", "日/西", "はるこ/Esmeralda", SourceType.VOLCANO),
        VoiceBean("multi_male_jingqiangkanye_moon_bigtts", "日/西", "かずね/Javier or Álvaro", SourceType.VOLCANO),
        VoiceBean("multi_female_gaolengyujie_moon_bigtts", "日语", "あけみ", SourceType.VOLCANO),
        VoiceBean("multi_male_wanqudashu_moon_bigtts", "日/西", "ひろし/Roberto", SourceType.VOLCANO),
        VoiceBean("ICL_zh_female_bingruoshaonv_tob", "中文", "病弱少女", SourceType.VOLCANO),
        VoiceBean("ICL_zh_female_huoponvhai_tob", "中文", "活泼女孩", SourceType.VOLCANO),
        VoiceBean("ICL_zh_female_heainainai_tob", "中文", "和蔼奶奶", SourceType.VOLCANO),
        VoiceBean("ICL_zh_female_linjuayi_tob", "中文", "邻居阿姨", SourceType.VOLCANO),
        VoiceBean("zh_female_wenrouxiaoya_moon_bigtts", "中文", "温柔小雅", SourceType.VOLCANO),
        VoiceBean("zh_female_tianmeixiaoyuan_moon_bigtts", "中文", "甜美小源", SourceType.VOLCANO),
        VoiceBean("zh_female_qingchezizi_moon_bigtts", "中文", "清澈梓梓", SourceType.VOLCANO),
        VoiceBean("zh_male_dongfanghaoran_moon_bigtts", "中文", "东方浩然", SourceType.VOLCANO),
        VoiceBean("zh_male_jieshuoxiaoming_moon_bigtts", "中文", "解说小明", SourceType.VOLCANO),
        VoiceBean("zh_female_kailangjiejie_moon_bigtts", "中文", "开朗姐姐", SourceType.VOLCANO),
        VoiceBean("zh_male_linjiananhai_moon_bigtts", "中文", "邻家男孩", SourceType.VOLCANO),
        VoiceBean("zh_female_tianmeiyueyue_moon_bigtts", "中文", "甜美悦悦", SourceType.VOLCANO),
        VoiceBean("zh_female_xinlingjitang_moon_bigtts", "中文", "心灵鸡汤", SourceType.VOLCANO),
        VoiceBean("zh_female_cancan_mars_bigtts", "中文", "灿灿", SourceType.VOLCANO),
        VoiceBean("en_female_anna_mars_bigtts", "英文", "Anna", SourceType.VOLCANO),
        VoiceBean("zh_male_tiancaitongsheng_mars_bigtts", "中文", "天才童声", SourceType.VOLCANO),
        VoiceBean("zh_male_naiqimengwa_mars_bigtts", "中文", "奶气萌娃", SourceType.VOLCANO),
        VoiceBean("zh_male_sunwukong_mars_bigtts", "中文", "猴哥", SourceType.VOLCANO),
        VoiceBean("zh_male_xionger_mars_bigtts", "中文", "熊二", SourceType.VOLCANO),
        VoiceBean("zh_female_peiqi_mars_bigtts", "中文", "佩奇猪", SourceType.VOLCANO),
        VoiceBean("zh_female_zhixingnvsheng_mars_bigtts", "中文", "知性女声", SourceType.VOLCANO),
        VoiceBean("zh_female_qingxinnvsheng_mars_bigtts", "中/英", "清新女声", SourceType.VOLCANO),
        VoiceBean("zh_male_changtianyi_mars_bigtts", "中文", "悬疑解说", SourceType.VOLCANO),
        VoiceBean("zh_female_popo_mars_bigtts", "中文", "婆婆", SourceType.VOLCANO),
        VoiceBean("zh_female_wuzetian_mars_bigtts", "中文", "武则天", SourceType.VOLCANO),
        VoiceBean("zh_female_shaoergushi_mars_bigtts", "中/英", "少儿故事", SourceType.VOLCANO),
        VoiceBean("zh_male_silang_mars_bigtts", "中/英", "四郎", SourceType.VOLCANO),
        VoiceBean("zh_female_gujie_mars_bigtts", "中文", "顾姐", SourceType.VOLCANO),
        VoiceBean("zh_female_yingtaowanzi_mars_bigtts", "中文", "樱桃丸子", SourceType.VOLCANO),
        VoiceBean("zh_male_jieshuonansheng_mars_bigtts", "中/英", "磁性解说男声", SourceType.VOLCANO),
        VoiceBean("zh_female_jitangmeimei_mars_bigtts", "中/英", "鸡汤妹妹", SourceType.VOLCANO),
        VoiceBean("zh_male_chunhui_mars_bigtts", "中文", "广告解说", SourceType.VOLCANO),
        VoiceBean("zh_male_qingshuangnanda_mars_bigtts", "中文", "清爽男大", SourceType.VOLCANO),
        VoiceBean("zh_female_tiexinnvsheng_mars_bigtts", "中/英", "贴心女声", SourceType.VOLCANO),
        VoiceBean("zh_female_qiaopinvsheng_mars_bigtts", "中/英", "俏皮女声", SourceType.VOLCANO),
        VoiceBean("zh_female_mengyatou_mars_bigtts", "中/英", "萌丫头", SourceType.VOLCANO),
        VoiceBean("en_male_adam_mars_bigtts", "英语", "Adam", SourceType.VOLCANO),
        VoiceBean("en_female_sarah_mars_bigtts", "英语", "Sarah", SourceType.VOLCANO),
        VoiceBean("en_male_dryw_mars_bigtts", "英语", "Dryw", SourceType.VOLCANO),
        VoiceBean("en_male_smith_mars_bigtts", "英语", "Smith", SourceType.VOLCANO),
        VoiceBean("zh_male_ruyaqingnian_mars_bigtts", "中文", "儒雅青年", SourceType.VOLCANO),
        VoiceBean("zh_male_baqiqingshu_mars_bigtts", "中文", "霸气青叔", SourceType.VOLCANO),
        VoiceBean("zh_male_qingcang_mars_bigtts", "中文", "擎苍", SourceType.VOLCANO),
        VoiceBean("zh_male_yangguangqingnian_mars_bigtts", "中文", "活力小哥", SourceType.VOLCANO),
        VoiceBean("zh_female_gufengshaoyu_mars_bigtts", "中文", "古风少御", SourceType.VOLCANO),
        VoiceBean("zh_female_wenroushunv_mars_bigtts", "中文", "温柔淑女", SourceType.VOLCANO),
        VoiceBean("ICL_zh_female_zhixingwenwan_tob", "中文", "知性温婉", SourceType.VOLCANO),
        VoiceBean("ICL_zh_male_lvchaxiaoge_tob", "中文", "绿茶小哥", SourceType.VOLCANO),
        VoiceBean("ICL_zh_female_jiaoruoluoli_tob", "中文", "娇弱萝莉", SourceType.VOLCANO),
        VoiceBean("ICL_zh_male_lengdanshuli_tob", "中文", "冷淡疏离", SourceType.VOLCANO),
        VoiceBean("ICL_zh_male_nuanxintitie_tob", "中文", "暖心体贴", SourceType.VOLCANO),
        VoiceBean("ICL_zh_male_hanhoudunshi_tob", "中文", "憨厚敦实", SourceType.VOLCANO),
        VoiceBean("ICL_zh_female_wenrouwenya_tob", "中文", "温柔文雅", SourceType.VOLCANO),
        VoiceBean("ICL_zh_male_aiqilingren_tob", "中文", "傲气凌人", SourceType.VOLCANO),
        VoiceBean("ICL_zh_male_kailangqingkuai_tob", "中文", "开朗轻快", SourceType.VOLCANO),
        VoiceBean("ICL_zh_female_huopodiaoman_tob", "中文", "活泼刁蛮", SourceType.VOLCANO),
        VoiceBean("ICL_zh_male_guzhibingjiao_tob", "中文", "固执病娇", SourceType.VOLCANO),
        VoiceBean("ICL_zh_male_huoposhuanglang_tob", "中文", "活泼爽朗", SourceType.VOLCANO),
        VoiceBean("ICL_zh_male_sajiaonianren_tob", "中文", "撒娇粘人", SourceType.VOLCANO),
        VoiceBean("ICL_zh_female_aomanjiaosheng_tob", "中文", "傲慢娇声", SourceType.VOLCANO),
        VoiceBean("ICL_zh_male_xiaosasuixing_tob", "中文", "潇洒随性", SourceType.VOLCANO),
        VoiceBean("ICL_zh_male_fuheigongzi_tob", "中文", "腹黑公子", SourceType.VOLCANO),
        VoiceBean("ICL_zh_male_guiyishenmi_tob", "中文", "诡异神秘", SourceType.VOLCANO),
        VoiceBean("ICL_zh_male_ruyacaijun_tob", "中文", "儒雅才俊", SourceType.VOLCANO),
        VoiceBean("ICL_zh_male_bingjiaobailian_tob", "中文", "病娇白莲", SourceType.VOLCANO),
        VoiceBean("ICL_zh_male_zhengzhiqingnian_tob", "中文", "正直青年", SourceType.VOLCANO),
        VoiceBean("ICL_zh_male_shuaizhenxiaohuo_tob", "中文", "率真小伙", SourceType.VOLCANO),
        VoiceBean("ICL_zh_female_jiaohannvwang_tob", "中文", "娇憨女王", SourceType.VOLCANO),
        VoiceBean("ICL_zh_female_bingjiaomengmei_tob", "中文", "病娇萌妹", SourceType.VOLCANO),
        VoiceBean("ICL_zh_male_qingsenaigou_tob", "中文", "青涩小生", SourceType.VOLCANO),
        VoiceBean("ICL_zh_male_chunzhenxuedi_tob", "中文", "纯真学弟", SourceType.VOLCANO),
        VoiceBean("ICL_zh_female_nuanxinxuejie_tob", "中文", "暖心学姐", SourceType.VOLCANO),
        VoiceBean("ICL_zh_female_keainvsheng_tob", "中文", "可爱女生", SourceType.VOLCANO),
        VoiceBean("ICL_zh_female_chengshujiejie_tob", "中文", "成熟姐姐", SourceType.VOLCANO),
        VoiceBean("ICL_zh_female_bingjiaojiejie_tob", "中文", "病娇姐姐", SourceType.VOLCANO),
        VoiceBean("ICL_zh_male_youroubangzhu_tob", "中文", "优柔帮主", SourceType.VOLCANO),
        VoiceBean("ICL_zh_male_yourougongzi_tob", "中文", "优柔公子", SourceType.VOLCANO),
        VoiceBean("ICL_zh_female_wumeiyujie_tob", "中文", "妩媚御姐", SourceType.VOLCANO),
        VoiceBean("ICL_zh_female_tiaopigongzhu_tob", "中文", "调皮公主", SourceType.VOLCANO),
        VoiceBean("ICL_zh_female_aojiaonvyou_tob", "中文", "傲娇女友", SourceType.VOLCANO),
        VoiceBean("ICL_zh_male_tiexinnanyou_tob", "中文", "贴心男友", SourceType.VOLCANO),
        VoiceBean("ICL_zh_male_shaonianjiangjun_tob", "中文", "少年将军", SourceType.VOLCANO),
        VoiceBean("ICL_zh_female_tiexinnvyou_tob", "中文", "贴心女友", SourceType.VOLCANO),
        VoiceBean("ICL_zh_male_bingjiaogege_tob", "中文", "病娇哥哥", SourceType.VOLCANO),
        VoiceBean("ICL_zh_male_xuebanantongzhuo_tob", "中文", "学霸男同桌", SourceType.VOLCANO),
        VoiceBean("ICL_zh_male_youmoshushu_tob", "中文", "幽默叔叔", SourceType.VOLCANO),
        VoiceBean("ICL_zh_female_xingganyujie_tob", "中文", "性感御姐", SourceType.VOLCANO),
        VoiceBean("ICL_zh_female_jiaxiaozi_tob", "中文", "假小子", SourceType.VOLCANO),
        VoiceBean("ICL_zh_male_lengjunshangsi_tob", "中文", "冷峻上司", SourceType.VOLCANO),
        VoiceBean("ICL_zh_male_wenrounantongzhuo_tob", "中文", "温柔男同桌", SourceType.VOLCANO),
        VoiceBean("ICL_zh_male_bingjiaodidi_tob", "中文", "病娇弟弟", SourceType.VOLCANO),
        VoiceBean("ICL_zh_male_youmodaye_tob", "中文", "幽默大爷", SourceType.VOLCANO),
        VoiceBean("ICL_zh_male_aomanshaoye_tob", "中文", "傲慢少爷", SourceType.VOLCANO),
        VoiceBean("ICL_zh_male_shenmifashi_tob", "中文", "神秘法师", SourceType.VOLCANO),
        VoiceBean("zh_male_fanjuanqingnian_mars_bigtts", "中文", "反卷青年", SourceType.VOLCANO),
        VoiceBean("en_male_jackson_mars_bigtts", "英语", "Jackson", SourceType.VOLCANO),
        VoiceBean("en_female_amanda_mars_bigtts", "英语", "Amanda", SourceType.VOLCANO),
        VoiceBean("zh_male_dongmanhaimian_mars_bigtts", "中文", "亮嗓萌仔", SourceType.VOLCANO),
        VoiceBean("zh_male_wenrouxiaoge_mars_bigtts", "中文", "温柔小哥", SourceType.VOLCANO),
        VoiceBean("zh_male_lanxiaoyang_mars_bigtts", "中文", "懒音绵宝", SourceType.VOLCANO),
        VoiceBean("zh_male_beijingxiaoye_emo_v2_mars_bigtts", "中文", "北京小爷（多情感）", SourceType.VOLCANO),
        VoiceBean("zh_female_roumeinvyou_emo_v2_mars_bigtts", "中文", "柔美女友（多情感）", SourceType.VOLCANO),
        VoiceBean("zh_male_yangguangqingnian_emo_v2_mars_bigtts", "中文", "阳光青年（多情感）", SourceType.VOLCANO),
        VoiceBean("zh_female_meilinvyou_emo_v2_mars_bigtts", "中文", "魅力女友（多情感）", SourceType.VOLCANO),
        VoiceBean("zh_female_shuangkuaisisi_emo_v2_mars_bigtts", "中文", "爽快思思（多情感）", SourceType.VOLCANO),
        VoiceBean("zh_female_tianmeitaozi_mars_bigtts", "中文", "甜美桃子", SourceType.VOLCANO),
        VoiceBean("zh_female_kefunvsheng_mars_bigtts", "中文", "暖阳女声", SourceType.VOLCANO),
        VoiceBean("zh_female_daimengchuanmei_moon_bigtts", "中文", "呆萌川妹", SourceType.VOLCANO),
        VoiceBean("zh_male_guozhoudege_moon_bigtts", "中文", "广州德哥", SourceType.VOLCANO),
        VoiceBean("zh_male_beijingxiaoye_moon_bigtts", "中文", "北京小爷", SourceType.VOLCANO),
        VoiceBean("zh_male_shaonianzixin_moon_bigtts", "中/英", "少年梓辛/Brayan", SourceType.VOLCANO),
        VoiceBean("zh_female_meilinvyou_moon_bigtts", "中文", "魅力女友", SourceType.VOLCANO),
        VoiceBean("zh_male_shenyeboke_moon_bigtts", "中文", "深夜播客", SourceType.VOLCANO),
        VoiceBean("zh_female_sajiaonvyou_moon_bigtts", "中文", "柔美女友", SourceType.VOLCANO),
        VoiceBean("zh_female_yuanqinvyou_moon_bigtts", "中文", "撒娇学妹", SourceType.VOLCANO),
        VoiceBean("zh_male_haoyuxiaoge_moon_bigtts", "中文", "浩宇小哥", SourceType.VOLCANO),
        VoiceBean("zh_male_guangxiyuanzhou_moon_bigtts", "中文", "广西远舟", SourceType.VOLCANO),
        VoiceBean("zh_female_meituojieer_moon_bigtts", "中文", "妹坨洁儿", SourceType.VOLCANO),
        VoiceBean("zh_male_yuzhouzixuan_moon_bigtts", "中文", "豫州子轩", SourceType.VOLCANO),
        VoiceBean("zh_female_linjianvhai_moon_bigtts", "中文", "邻家女孩", SourceType.VOLCANO),
        VoiceBean("zh_female_gaolengyujie_moon_bigtts", "中文", "高冷御姐", SourceType.VOLCANO),
        VoiceBean("zh_male_yuanboxiaoshu_moon_bigtts", "中文", "渊博小叔", SourceType.VOLCANO),
        VoiceBean("zh_male_yangguangqingnian_moon_bigtts", "中文", "阳光青年", SourceType.VOLCANO),
        VoiceBean("zh_male_aojiaobazong_moon_bigtts", "中文", "傲娇霸总", SourceType.VOLCANO),
        VoiceBean("zh_male_jingqiangkanye_moon_bigtts", "中/英", "京腔侃爷/Harmony", SourceType.VOLCANO),
        VoiceBean("zh_female_shuangkuaisisi_moon_bigtts", "中/英", "爽快思思/Skye", SourceType.VOLCANO),
        VoiceBean("zh_male_wennuanahu_moon_bigtts", "中/英", "温暖阿虎/Alvin", SourceType.VOLCANO),
        VoiceBean("zh_female_wanwanxiaohe_moon_bigtts", "中文", "湾湾小何", SourceType.VOLCANO),
        VoiceBean("multi_female_shuangkuaisisi_moon_bigtts", "日/西", "はるこ/Esmeralda", SourceType.VOLCANO),
        VoiceBean("multi_male_jingqiangkanye_moon_bigtts", "日/西", "かずね/Javier or Álvaro", SourceType.VOLCANO),
        VoiceBean("multi_female_gaolengyujie_moon_bigtts", "日语", "あけみ", SourceType.VOLCANO),
        VoiceBean("multi_male_wanqudashu_moon_bigtts", "日/西", "ひろし/Roberto", SourceType.VOLCANO),
        VoiceBean("ICL_zh_female_bingruoshaonv_tob", "中文", "病弱少女", SourceType.VOLCANO),
        VoiceBean("ICL_zh_female_huoponvhai_tob", "中文", "活泼女孩", SourceType.VOLCANO),
        VoiceBean("ICL_zh_female_heainainai_tob", "中文", "和蔼奶奶", SourceType.VOLCANO),
        VoiceBean("ICL_zh_female_linjuayi_tob", "中文", "邻居阿姨", SourceType.VOLCANO),
        VoiceBean("zh_female_wenrouxiaoya_moon_bigtts", "中文", "温柔小雅", SourceType.VOLCANO),
        VoiceBean("zh_female_tianmeixiaoyuan_moon_bigtts", "中文", "甜美小源", SourceType.VOLCANO),
        VoiceBean("zh_female_qingchezizi_moon_bigtts", "中文", "清澈梓梓", SourceType.VOLCANO),
        VoiceBean("zh_male_dongfanghaoran_moon_bigtts", "中文", "东方浩然", SourceType.VOLCANO),
        VoiceBean("zh_male_jieshuoxiaoming_moon_bigtts", "中文", "解说小明", SourceType.VOLCANO),
        VoiceBean("zh_female_kailangjiejie_moon_bigtts", "中文", "开朗姐姐", SourceType.VOLCANO),
        VoiceBean("zh_male_linjiananhai_moon_bigtts", "中文", "邻家男孩", SourceType.VOLCANO),
        VoiceBean("zh_female_tianmeiyueyue_moon_bigtts", "中文", "甜美悦悦", SourceType.VOLCANO),
        VoiceBean("zh_female_xinlingjitang_moon_bigtts", "中文", "心灵鸡汤", SourceType.VOLCANO),
        VoiceBean("zh_female_cancan_mars_bigtts", "中文", "灿灿", SourceType.VOLCANO),
        VoiceBean("en_female_anna_mars_bigtts", "英文", "Anna", SourceType.VOLCANO),
        VoiceBean("zh_male_tiancaitongsheng_mars_bigtts", "中文", "天才童声", SourceType.VOLCANO),
        VoiceBean("zh_male_naiqimengwa_mars_bigtts", "中文", "奶气萌娃", SourceType.VOLCANO),
        VoiceBean("zh_male_sunwukong_mars_bigtts", "中文", "猴哥", SourceType.VOLCANO),
        VoiceBean("zh_male_xionger_mars_bigtts", "中文", "熊二", SourceType.VOLCANO),
        VoiceBean("zh_female_peiqi_mars_bigtts", "中文", "佩奇猪", SourceType.VOLCANO),
        VoiceBean("zh_female_zhixingnvsheng_mars_bigtts", "中文", "知性女声", SourceType.VOLCANO),
        VoiceBean("zh_female_qingxinnvsheng_mars_bigtts", "中/英", "清新女声", SourceType.VOLCANO),
        VoiceBean("zh_male_changtianyi_mars_bigtts", "中文", "悬疑解说", SourceType.VOLCANO),
        VoiceBean("zh_female_popo_mars_bigtts", "中文", "婆婆", SourceType.VOLCANO),
        VoiceBean("zh_female_wuzetian_mars_bigtts", "中文", "武则天", SourceType.VOLCANO),
        VoiceBean("zh_female_shaoergushi_mars_bigtts", "中/英", "少儿故事", SourceType.VOLCANO),
        VoiceBean("zh_male_silang_mars_bigtts", "中/英", "四郎", SourceType.VOLCANO),
        VoiceBean("zh_female_gujie_mars_bigtts", "中文", "顾姐", SourceType.VOLCANO),
        VoiceBean("zh_female_yingtaowanzi_mars_bigtts", "中文", "樱桃丸子", SourceType.VOLCANO),
        VoiceBean("zh_male_jieshuonansheng_mars_bigtts", "中/英", "磁性解说男声", SourceType.VOLCANO),
        VoiceBean("zh_female_jitangmeimei_mars_bigtts", "中/英", "鸡汤妹妹", SourceType.VOLCANO),
        VoiceBean("zh_male_chunhui_mars_bigtts", "中文", "广告解说", SourceType.VOLCANO),
        VoiceBean("zh_male_qingshuangnanda_mars_bigtts", "中文", "清爽男大", SourceType.VOLCANO),
        VoiceBean("zh_female_tiexinnvsheng_mars_bigtts", "中/英", "贴心女声", SourceType.VOLCANO),
        VoiceBean("zh_female_qiaopinvsheng_mars_bigtts", "中/英", "俏皮女声", SourceType.VOLCANO),
        VoiceBean("zh_female_mengyatou_mars_bigtts", "中/英", "萌丫头", SourceType.VOLCANO),
        VoiceBean("en_male_adam_mars_bigtts", "英语", "Adam", SourceType.VOLCANO),
        VoiceBean("en_female_sarah_mars_bigtts", "英语", "Sarah", SourceType.VOLCANO),
        VoiceBean("en_male_dryw_mars_bigtts", "英语", "Dryw", SourceType.VOLCANO),
        VoiceBean("en_male_smith_mars_bigtts", "英语", "Smith", SourceType.VOLCANO),
        VoiceBean("zh_male_ruyaqingnian_mars_bigtts", "中文", "儒雅青年", SourceType.VOLCANO),
        VoiceBean("zh_male_baqiqingshu_mars_bigtts", "中文", "霸气青叔", SourceType.VOLCANO),
        VoiceBean("zh_male_qingcang_mars_bigtts", "中文", "擎苍", SourceType.VOLCANO),
        VoiceBean("zh_male_yangguangqingnian_mars_bigtts", "中文", "活力小哥", SourceType.VOLCANO),
        VoiceBean("zh_female_gufengshaoyu_mars_bigtts", "中文", "古风少御", SourceType.VOLCANO),
        VoiceBean("zh_female_wenroushunv_mars_bigtts", "中文", "温柔淑女", SourceType.VOLCANO),
        VoiceBean("ICL_zh_female_zhixingwenwan_tob", "中文", "知性温婉", SourceType.VOLCANO),
        VoiceBean("ICL_zh_male_lvchaxiaoge_tob", "中文", "绿茶小哥", SourceType.VOLCANO),
        VoiceBean("ICL_zh_female_jiaoruoluoli_tob", "中文", "娇弱萝莉", SourceType.VOLCANO),
        VoiceBean("ICL_zh_male_lengdanshuli_tob", "中文", "冷淡疏离", SourceType.VOLCANO),
        VoiceBean("ICL_zh_male_nuanxintitie_tob", "中文", "暖心体贴", SourceType.VOLCANO),
        VoiceBean("ICL_zh_male_hanhoudunshi_tob", "中文", "憨厚敦实", SourceType.VOLCANO),
        VoiceBean("ICL_zh_female_wenrouwenya_tob", "中文", "温柔文雅", SourceType.VOLCANO),
        VoiceBean("ICL_zh_male_aiqilingren_tob", "中文", "傲气凌人", SourceType.VOLCANO),
        VoiceBean("ICL_zh_male_kailangqingkuai_tob", "中文", "开朗轻快", SourceType.VOLCANO),
        VoiceBean("ICL_zh_female_huopodiaoman_tob", "中文", "活泼刁蛮", SourceType.VOLCANO),
        VoiceBean("ICL_zh_male_guzhibingjiao_tob", "中文", "固执病娇", SourceType.VOLCANO),
        VoiceBean("ICL_zh_male_huoposhuanglang_tob", "中文", "活泼爽朗", SourceType.VOLCANO),
        VoiceBean("ICL_zh_male_sajiaonianren_tob", "中文", "撒娇粘人", SourceType.VOLCANO),
        VoiceBean("ICL_zh_female_aomanjiaosheng_tob", "中文", "傲慢娇声", SourceType.VOLCANO),
        VoiceBean("ICL_zh_male_xiaosasuixing_tob", "中文", "潇洒随性", SourceType.VOLCANO),
        VoiceBean("ICL_zh_male_fuheigongzi_tob", "中文", "腹黑公子", SourceType.VOLCANO),
        VoiceBean("ICL_zh_male_guiyishenmi_tob", "中文", "诡异神秘", SourceType.VOLCANO),
        VoiceBean("ICL_zh_male_ruyacaijun_tob", "中文", "儒雅才俊", SourceType.VOLCANO),
        VoiceBean("ICL_zh_male_bingjiaobailian_tob", "中文", "病娇白莲", SourceType.VOLCANO),
        VoiceBean("ICL_zh_male_zhengzhiqingnian_tob", "中文", "正直青年", SourceType.VOLCANO),
        VoiceBean("ICL_zh_male_shuaizhenxiaohuo_tob", "中文", "率真小伙", SourceType.VOLCANO),
        VoiceBean("ICL_zh_female_jiaohannvwang_tob", "中文", "娇憨女王", SourceType.VOLCANO),
        VoiceBean("ICL_zh_female_bingjiaomengmei_tob", "中文", "病娇萌妹", SourceType.VOLCANO),
        VoiceBean("ICL_zh_male_qingsenaigou_tob", "中文", "青涩小生", SourceType.VOLCANO),
        VoiceBean("ICL_zh_male_chunzhenxuedi_tob", "中文", "纯真学弟", SourceType.VOLCANO),
        VoiceBean("ICL_zh_female_nuanxinxuejie_tob", "中文", "暖心学姐", SourceType.VOLCANO),
        VoiceBean("ICL_zh_female_keainvsheng_tob", "中文", "可爱女生", SourceType.VOLCANO),
        VoiceBean("ICL_zh_female_chengshujiejie_tob", "中文", "成熟姐姐", SourceType.VOLCANO),
        VoiceBean("ICL_zh_female_bingjiaojiejie_tob", "中文", "病娇姐姐", SourceType.VOLCANO),
        VoiceBean("ICL_zh_male_youroubangzhu_tob", "中文", "优柔帮主", SourceType.VOLCANO),
        VoiceBean("ICL_zh_male_yourougongzi_tob", "中文", "优柔公子", SourceType.VOLCANO),
        VoiceBean("ICL_zh_female_wumeiyujie_tob", "中文", "妩媚御姐", SourceType.VOLCANO),
        VoiceBean("ICL_zh_female_tiaopigongzhu_tob", "中文", "调皮公主", SourceType.VOLCANO),
        VoiceBean("ICL_zh_female_aojiaonvyou_tob", "中文", "傲娇女友", SourceType.VOLCANO),
        VoiceBean("ICL_zh_male_tiexinnanyou_tob", "中文", "贴心男友", SourceType.VOLCANO),
        VoiceBean("ICL_zh_male_shaonianjiangjun_tob", "中文", "少年将军", SourceType.VOLCANO),
        VoiceBean("ICL_zh_female_tiexinnvyou_tob", "中文", "贴心女友", SourceType.VOLCANO),
        VoiceBean("ICL_zh_male_bingjiaogege_tob", "中文", "病娇哥哥", SourceType.VOLCANO),
        VoiceBean("ICL_zh_male_xuebanantongzhuo_tob", "中文", "学霸男同桌", SourceType.VOLCANO),
        VoiceBean("ICL_zh_male_youmoshushu_tob", "中文", "幽默叔叔", SourceType.VOLCANO),
        VoiceBean("ICL_zh_female_xingganyujie_tob", "中文", "性感御姐", SourceType.VOLCANO),
        VoiceBean("ICL_zh_female_jiaxiaozi_tob", "中文", "假小子", SourceType.VOLCANO),
        VoiceBean("ICL_zh_male_lengjunshangsi_tob", "中文", "冷峻上司", SourceType.VOLCANO),
        VoiceBean("ICL_zh_male_wenrounantongzhuo_tob", "中文", "温柔男同桌", SourceType.VOLCANO),
        VoiceBean("ICL_zh_male_bingjiaodidi_tob", "中文", "病娇弟弟", SourceType.VOLCANO),
        VoiceBean("ICL_zh_male_youmodaye_tob", "中文", "幽默大爷", SourceType.VOLCANO),
        VoiceBean("ICL_zh_male_aomanshaoye_tob", "中文", "傲慢少爷", SourceType.VOLCANO),
        VoiceBean("ICL_zh_male_shenmifashi_tob", "中文", "神秘法师", SourceType.VOLCANO),
        VoiceBean("zh_male_fanjuanqingnian_mars_bigtts", "中文", "反卷青年", SourceType.VOLCANO),
        VoiceBean("en_male_jackson_mars_bigtts", "英语", "Jackson", SourceType.VOLCANO),
        VoiceBean("en_female_amanda_mars_bigtts", "英语", "Amanda", SourceType.VOLCANO),
        VoiceBean("zh_male_dongmanhaimian_mars_bigtts", "中文", "亮嗓萌仔", SourceType.VOLCANO),
        VoiceBean("zh_male_wenrouxiaoge_mars_bigtts", "中文", "温柔小哥", SourceType.VOLCANO),
        VoiceBean("zh_male_lanxiaoyang_mars_bigtts", "中文", "懒音绵宝", SourceType.VOLCANO),
        VoiceBean("zh_male_beijingxiaoye_emo_v2_mars_bigtts", "中文", "北京小爷（多情感）", SourceType.VOLCANO),
        VoiceBean("zh_female_roumeinvyou_emo_v2_mars_bigtts", "中文", "柔美女友（多情感）", SourceType.VOLCANO),
        VoiceBean("zh_male_yangguangqingnian_emo_v2_mars_bigtts", "中文", "阳光青年（多情感）", SourceType.VOLCANO),
        VoiceBean("zh_female_meilinvyou_emo_v2_mars_bigtts", "中文", "魅力女友（多情感）", SourceType.VOLCANO),
        VoiceBean("zh_female_shuangkuaisisi_emo_v2_mars_bigtts", "中文", "爽快思思（多情感）", SourceType.VOLCANO),
        VoiceBean("zh_female_tianmeitaozi_mars_bigtts", "中文", "甜美桃子", SourceType.VOLCANO),
        VoiceBean("zh_female_kefunvsheng_mars_bigtts", "中文", "暖阳女声", SourceType.VOLCANO),
        VoiceBean("ICL_zh_female_ganli_v1_tob", "中文", "妩媚可人", SourceType.VOLCANO),
        VoiceBean("ICL_zh_male_flc_v1_tob", "中文", "儒雅公子", SourceType.VOLCANO),
        VoiceBean("ICL_zh_female_luoqing_v1_tob", "中文", "性感魅惑", SourceType.VOLCANO),
        VoiceBean("zh_female_qinqienvsheng_moon_bigtts", "中文", "亲切女声", SourceType.VOLCANO),
        VoiceBean("ICL_zh_male_lengkugege_v1_tob", "中文", "冷酷哥哥", SourceType.VOLCANO),
        VoiceBean("multi_male_xudong_conversation_wvae_bigtts", "日/西", "まさお（正男）/Daníel", SourceType.VOLCANO),
        VoiceBean("multi_female_sophie_conversation_wvae_bigtts", "日/西", "さとみ（智美）/Sofía", SourceType.VOLCANO),
        VoiceBean("zh_male_M100_conversation_wvae_bigtts", "中/英", "悠悠君子/Lucas", SourceType.VOLCANO),
        VoiceBean("zh_male_xudong_conversation_wvae_bigtts", "中/英", "快乐小东/Daniel", SourceType.VOLCANO),
        VoiceBean("zh_female_maomao_conversation_wvae_bigtts", "中文", "文静毛毛", SourceType.VOLCANO),
        VoiceBean("zh_male_qingyiyuxuan_mars_bigtts", "中文", "阳光阿辰", SourceType.VOLCANO),
        VoiceBean("zh_female_sophie_conversation_wvae_bigtts", "中/英", "魅力苏菲/Sophie", SourceType.VOLCANO),
        VoiceBean("ICL_zh_female_bingjiao3_tob", "中文", "邪魅女王", SourceType.VOLCANO),
        VoiceBean("en_female_emily_mars_bigtts", "英文", "Emily", SourceType.VOLCANO),
        VoiceBean("multi_zh_male_youyoujunzi_moon_bigtts", "日文", "ひかる（光）", SourceType.VOLCANO),
        VoiceBean("ICL_en_male_cc_sha_v1_tob", "英文", "Cartoon Chef", SourceType.VOLCANO),
        VoiceBean("ICL_zh_male_yangyang_v1_tob", "中文", "温暖少年", SourceType.VOLCANO),
        VoiceBean("ICL_zh_female_wenyinvsheng_v1_tob", "中文", "知心姐姐", SourceType.VOLCANO),
        VoiceBean("ICL_zh_male_BV144_paoxiaoge_v1_tob", "中文", "咆哮小哥", SourceType.VOLCANO),
        VoiceBean("zh_male_junlangnanyou_emo_v2_mars_bigtts", "中文", "俊朗男友（多情感）", SourceType.VOLCANO),
        VoiceBean("zh_male_ruyayichen_emo_v2_mars_bigtts", "中文", "儒雅男友", SourceType.VOLCANO),
        VoiceBean("ICL_en_male_cc_alastor_tob", "英式英语", "Alastor", SourceType.VOLCANO),
        VoiceBean("ICL_en_female_cc_cm_v1_tob", "英文", "Charlie", SourceType.VOLCANO),
        VoiceBean("ICL_en_male_cc_dracula_v1_tob", "英文", "Noah", SourceType.VOLCANO),
        VoiceBean("ICL_en_male_xavier1_v1_tob", "英文", "Xavier", SourceType.VOLCANO),
        VoiceBean("ICL_en_male_cc_penny_v1_tob", "英文", "Clown Man", SourceType.VOLCANO),
        VoiceBean("ICL_en_male_cc_chucky_tob", "英文", "Chucky", SourceType.VOLCANO),
        VoiceBean("ICL_en_male_aussie_v1_tob", "澳洲英语", "Ethan", SourceType.VOLCANO),
        VoiceBean("ICL_en_male_cc_jigsaw_tob", "英文", "Jigsaw", SourceType.VOLCANO),
        VoiceBean("ICL_en_male_zayne_tob", "英文", "Zayne", SourceType.VOLCANO),
        VoiceBean("ICL_en_male_grinch2_tob", "英文", "The Grinch", SourceType.VOLCANO),
        VoiceBean("ICL_en_male_frosty1_tob", "英文", "Frosty Man", SourceType.VOLCANO),
        VoiceBean("ICL_en_male_oogie2_tob", "英文", "Big Boogie", SourceType.VOLCANO),
        VoiceBean("ICL_en_male_michael_tob", "英文", "Michael", SourceType.VOLCANO),
        VoiceBean("ICL_zh_male_bingruogongzi_tob", "中文", "病弱公子", SourceType.VOLCANO),
        VoiceBean("ICL_en_male_kevin2_tob", "英文", "Kevin McCallister", SourceType.VOLCANO),
        VoiceBean("en_male_jason_conversation_wvae_bigtts", "中文", "开朗学长", SourceType.VOLCANO),
        VoiceBean("en_male_charlie_conversation_wvae_bigtts", "英文", "Owen", SourceType.VOLCANO),
        VoiceBean("en_female_sarah_new_conversation_wvae_bigtts", "英文", "Luna", SourceType.VOLCANO),
        VoiceBean("zh_male_zhoujielun_emo_v2_mars_bigtts", "中文-台湾口音", "双节棍小哥", SourceType.VOLCANO),
        VoiceBean("zh_male_yourougongzi_emo_v2_mars_bigtts", "中文", "优柔公子（多情感）", SourceType.VOLCANO),
        VoiceBean("zh_female_linjuayi_emo_v2_mars_bigtts", "中文", "邻居阿姨（多情感）", SourceType.VOLCANO),
        VoiceBean("zh_male_jingqiangkanye_emo_mars_bigtts", "中文", "京腔侃爷（多情感）", SourceType.VOLCANO),
        VoiceBean("zh_male_guangzhoudege_emo_mars_bigtts", "中文", "广州德哥（多情感）", SourceType.VOLCANO),
        VoiceBean("zh_male_aojiaobazong_emo_v2_mars_bigtts", "中文", "傲娇霸总（多情感）", SourceType.VOLCANO),
        VoiceBean("zh_female_tianxinxiaomei_emo_v2_mars_bigtts", "中文", "甜心小美（多情感）", SourceType.VOLCANO),
        VoiceBean("zh_female_gaolengyujie_emo_v2_mars_bigtts", "中文", "高冷御姐（多情感）", SourceType.VOLCANO),
        VoiceBean("ICL_zh_female_wuxi_tob", "中文", "元气甜妹", SourceType.VOLCANO),
        VoiceBean("ICL_zh_male_you_tob", "中文", "油腻大叔", SourceType.VOLCANO),
        VoiceBean("ICL_zh_male_ms_tob", "中文", "嚣张小哥", SourceType.VOLCANO),
        VoiceBean("ICL_zh_female_yilin_tob", "中文", "贴心妹妹", SourceType.VOLCANO),
        VoiceBean("ICL_zh_female_yry_tob", "中文", "温柔白月光", SourceType.VOLCANO),
        VoiceBean("ICL_zh_female_xnx_tob", "中文", "贴心闺蜜", SourceType.VOLCANO),
        VoiceBean("ICL_zh_female_yuxin_v1_tob", "中文", "初恋女友", SourceType.VOLCANO),
        VoiceBean("ICL_zh_female_qiuling_v1_tob", "中文", "倾心少女", SourceType.VOLCANO),
        VoiceBean("ICL_zh_male_shenmi_v1_tob", "中文", "机灵小伙", SourceType.VOLCANO),
        VoiceBean("ICL_zh_male_huzi_v1_tob", "中文", "胡子叔叔", SourceType.VOLCANO),
        VoiceBean("ICL_zh_male_buyan_v1_tob", "中文", "醇厚低音", SourceType.VOLCANO),
        VoiceBean("ICL_zh_female_feicui_v1_tob", "中文", "纯澈女生", SourceType.VOLCANO),
        VoiceBean("en_female_dacey_conversation_wvae_bigtts", "英文", "Daisy", SourceType.VOLCANO),
        VoiceBean("ICL_zh_female_xiangliangya_v1_tob", "中文", "邪魅御姐", SourceType.VOLCANO),
        VoiceBean("ICL_zh_male_guaogongzi_v1_tob", "中文", "孤傲公子", SourceType.VOLCANO),
        VoiceBean("ICL_zh_male_asmryexiu_tob", "中文", "枕边低语", SourceType.VOLCANO),
        VoiceBean("multi_female_maomao_conversation_wvae_bigtts", "日/西", "つき（月）/Diana", SourceType.VOLCANO),
        VoiceBean("multi_male_M100_conversation_wvae_bigtts", "西语", "Lucía", SourceType.VOLCANO),
        VoiceBean("ICL_zh_male_aomanqingnian_tob", "中文", "傲慢青年", SourceType.VOLCANO),
        VoiceBean("ICL_zh_male_cujingnanyou_tob", "中文", "醋精男友", SourceType.VOLCANO),
        VoiceBean("ICL_zh_male_cujingnansheng_tob", "中文", "醋精男生", SourceType.VOLCANO),
        VoiceBean("ICL_zh_male_shuanglangshaonian_tob", "中文", "爽朗少年", SourceType.VOLCANO),
        VoiceBean("ICL_zh_male_sajiaonanyou_tob", "中文", "撒娇男友", SourceType.VOLCANO),
        VoiceBean("ICL_zh_male_wenrounanyou_tob", "中文", "温柔男友", SourceType.VOLCANO),
        VoiceBean("ICL_zh_male_wenshunshaonian_tob", "中文", "温顺少年", SourceType.VOLCANO),
        VoiceBean("ICL_zh_male_naigounanyou_tob", "中文", "粘人男友", SourceType.VOLCANO),
        VoiceBean("ICL_zh_male_sajiaonansheng_tob", "中文", "撒娇男生", SourceType.VOLCANO),
        VoiceBean("ICL_zh_male_huoponanyou_tob", "中文", "活泼男友", SourceType.VOLCANO),
        VoiceBean("ICL_zh_male_tianxinanyou_tob", "中文", "甜系男友", SourceType.VOLCANO),
        VoiceBean("ICL_zh_male_huoliqingnian_tob", "中文", "活力青年", SourceType.VOLCANO),
        VoiceBean("ICL_zh_male_kailangqingnian_tob", "中文", "开朗青年", SourceType.VOLCANO),
        VoiceBean("ICL_zh_male_lengmoxiongzhang_tob", "中文", "冷漠兄长", SourceType.VOLCANO),
        VoiceBean("ICL_zh_male_tiancaitongzhuo_tob", "中文", "天才同桌", SourceType.VOLCANO),
        VoiceBean("ICL_zh_male_aojiaojingying_tob", "中文", "傲娇精英", SourceType.VOLCANO),
        VoiceBean("ICL_zh_male_pianpiangongzi_tob", "中文", "翩翩公子", SourceType.VOLCANO),
        VoiceBean("ICL_zh_male_mengdongqingnian_tob", "中文", "懵懂青年", SourceType.VOLCANO),
        VoiceBean("ICL_zh_male_lenglianxiongzhang_tob", "中文", "冷脸兄长", SourceType.VOLCANO),
        VoiceBean("ICL_zh_male_bingjiaoshaonian_tob", "中文", "病娇少年", SourceType.VOLCANO),
        VoiceBean("ICL_zh_male_bingjiaonanyou_tob", "中文", "病娇男友", SourceType.VOLCANO),
        VoiceBean("ICL_zh_male_bingruoshaonian_tob", "中文", "病弱少年", SourceType.VOLCANO),
        VoiceBean("ICL_zh_male_yiqishaonian_tob", "中文", "意气少年", SourceType.VOLCANO),
        VoiceBean("ICL_zh_male_ganjingshaonian_tob", "中文", "干净少年", SourceType.VOLCANO),
        VoiceBean("ICL_zh_male_lengmonanyou_tob", "中文", "冷漠男友", SourceType.VOLCANO),
        VoiceBean("ICL_zh_male_jingyingqingnian_tob", "中文", "精英青年", SourceType.VOLCANO),
        VoiceBean("ICL_zh_male_fengfashaonian_tob", "中文", "风发少年", SourceType.VOLCANO),
        VoiceBean("ICL_zh_male_rexueshaonian_tob", "中文", "热血少年", SourceType.VOLCANO),
        VoiceBean("ICL_zh_male_qingshuangshaonian_tob", "中文", "清爽少年", SourceType.VOLCANO),
        VoiceBean("ICL_zh_male_zhongerqingnian_tob", "中文", "中二青年", SourceType.VOLCANO),
        VoiceBean("ICL_zh_male_lingyunqingnian_tob", "中文", "凌云青年", SourceType.VOLCANO),
        VoiceBean("ICL_zh_male_zifuqingnian_tob", "中文", "自负青年", SourceType.VOLCANO),
        VoiceBean("ICL_zh_male_bujiqingnian_tob", "中文", "不羁青年", SourceType.VOLCANO),
        VoiceBean("ICL_zh_male_ruyajunzi_tob", "中文", "儒雅君子", SourceType.VOLCANO),
        VoiceBean("ICL_zh_male_diyinchenyu_tob", "中文", "低音沉郁", SourceType.VOLCANO),
        VoiceBean("ICL_zh_male_lenglianxueba_tob", "中文", "冷脸学霸", SourceType.VOLCANO),
        VoiceBean("ICL_zh_male_ruyazongcai_tob", "中文", "儒雅总裁", SourceType.VOLCANO),
        VoiceBean("ICL_zh_male_shenchenzongcai_tob", "中文", "深沉总裁", SourceType.VOLCANO),
        VoiceBean("ICL_zh_male_xiaohouye_tob", "中文", "小侯爷", SourceType.VOLCANO),
        VoiceBean("ICL_zh_male_gugaogongzi_tob", "中文", "孤高公子", SourceType.VOLCANO),
        VoiceBean("ICL_zh_male_zhangjianjunzi_tob", "中文", "仗剑君子", SourceType.VOLCANO),
        VoiceBean("ICL_zh_male_wenrunxuezhe_tob", "中文", "温润学者", SourceType.VOLCANO),
        VoiceBean("ICL_zh_male_qinqieqingnian_tob", "中文", "亲切青年", SourceType.VOLCANO),
        VoiceBean("ICL_zh_male_wenrouxuezhang_tob", "中文", "温柔学长", SourceType.VOLCANO),
        VoiceBean("ICL_zh_male_gaolengzongcai_tob", "中文", "高冷总裁", SourceType.VOLCANO),
        VoiceBean("ICL_zh_male_lengjungaozhi_tob", "中文", "冷峻高智", SourceType.VOLCANO),
        VoiceBean("ICL_zh_male_chanruoshaoye_tob", "中文", "孱弱少爷", SourceType.VOLCANO),
        VoiceBean("ICL_zh_male_zixinqingnian_tob", "中文", "自信青年", SourceType.VOLCANO),
        VoiceBean("ICL_zh_male_qingseqingnian_tob", "中文", "青涩青年", SourceType.VOLCANO),
        VoiceBean("ICL_zh_male_xuebatongzhuo_tob", "中文", "学霸同桌", SourceType.VOLCANO),
        VoiceBean("ICL_zh_male_lengaozongcai_tob", "中文", "冷傲总裁", SourceType.VOLCANO),
        VoiceBean("ICL_zh_male_badaoshaoye_tob", "中文", "霸道少爷", SourceType.VOLCANO),
        VoiceBean("ICL_zh_male_yuanqishaonian_tob", "中文", "元气少年", SourceType.VOLCANO),
        VoiceBean("ICL_zh_male_satuoqingnian_tob", "中文", "洒脱青年", SourceType.VOLCANO),
        VoiceBean("ICL_zh_male_zhishuaiqingnian_tob", "中文", "直率青年", SourceType.VOLCANO),
        VoiceBean("ICL_zh_male_siwenqingnian_tob", "中文", "斯文青年", SourceType.VOLCANO),
        VoiceBean("ICL_zh_male_chengshuzongcai_tob", "中文", "成熟总裁", SourceType.VOLCANO),
        VoiceBean("ICL_zh_male_junyigongzi_tob", "中文", "俊逸公子", SourceType.VOLCANO),
        VoiceBean("ICL_zh_male_aojiaogongzi_tob", "中文", "傲娇公子", SourceType.VOLCANO),
        VoiceBean("ICL_zh_male_zhangjianxiake_tob", "中文", "仗剑侠客", SourceType.VOLCANO),
        VoiceBean("ICL_zh_male_jijiaozhineng_tob", "中文", "机甲智能", SourceType.VOLCANO),
        VoiceBean("zh_female_vv_uranus_bigtts", "中文、日文、印尼、墨西哥西班牙语", "Vivi 2.0", SourceType.VOLCANO),
        VoiceBean("zh_female_xiaohe_uranus_bigtts", "中文", "小何 2.0", SourceType.VOLCANO),
        VoiceBean("zh_male_m191_uranus_bigtts", "中文", "云舟 2.0", SourceType.VOLCANO),
        VoiceBean("zh_male_taocheng_uranus_bigtts", "中文", "小天 2.0", SourceType.VOLCANO),
        VoiceBean("zh_male_liufei_uranus_bigtts", "中文", "刘飞 2.0", SourceType.VOLCANO),
        VoiceBean("zh_female_sophie_uranus_bigtts", "中文", "魅力苏菲 2.0", SourceType.VOLCANO),
        VoiceBean("zh_female_qingxinnvsheng_uranus_bigtts", "中文", "清新女声 2.0", SourceType.VOLCANO),
        VoiceBean("zh_female_cancan_uranus_bigtts", "中文", "知性灿灿 2.0", SourceType.VOLCANO),
        VoiceBean("zh_female_sajiaoxuemei_uranus_bigtts", "中文", "撒娇学妹 2.0", SourceType.VOLCANO),
        VoiceBean("zh_female_tianmeixiaoyuan_uranus_bigtts", "中文", "甜美小源 2.0", SourceType.VOLCANO),
        VoiceBean("zh_female_tianmeitaozi_uranus_bigtts", "中文", "甜美桃子 2.0", SourceType.VOLCANO),
        VoiceBean("zh_female_shuangkuaisisi_uranus_bigtts", "中文", "爽快思思 2.0", SourceType.VOLCANO),
        VoiceBean("zh_female_peiqi_uranus_bigtts", "中文", "佩奇猪 2.0", SourceType.VOLCANO),
        VoiceBean("zh_female_linjianvhai_uranus_bigtts", "中文", "邻家女孩 2.0", SourceType.VOLCANO),
        VoiceBean("zh_male_shaonianzixin_uranus_bigtts", "中文", "少年梓辛 2.0", SourceType.VOLCANO),
        VoiceBean("zh_male_sunwukong_uranus_bigtts", "中文", "猴哥 2.0", SourceType.VOLCANO),
        VoiceBean("zh_female_yingyujiaoxue_uranus_bigtts", "中文、英式英语", "Tina老师 2.0", SourceType.VOLCANO),
        VoiceBean("zh_female_kefunvsheng_uranus_bigtts", "中文", "暖阳女声 2.0", SourceType.VOLCANO),
        VoiceBean("zh_female_xiaoxue_uranus_bigtts", "中文", "儿童绘本 2.0", SourceType.VOLCANO),
        VoiceBean("zh_male_dayi_uranus_bigtts", "中文", "大壹 2.0", SourceType.VOLCANO),
        VoiceBean("zh_female_mizai_uranus_bigtts", "中文", "黑猫侦探社咪仔 2.0", SourceType.VOLCANO),
        VoiceBean("zh_female_jitangnv_uranus_bigtts", "中文", "鸡汤女 2.0", SourceType.VOLCANO),
        VoiceBean("zh_female_meilinvyou_uranus_bigtts", "中文", "魅力女友 2.0", SourceType.VOLCANO),
        VoiceBean("zh_female_liuchangnv_uranus_bigtts", "中文", "流畅女声 2.0", SourceType.VOLCANO),
        VoiceBean("zh_male_ruyayichen_uranus_bigtts", "中文", "儒雅逸辰 2.0", SourceType.VOLCANO),
        VoiceBean("en_male_tim_uranus_bigtts", "美式英语", "Tim", SourceType.VOLCANO),
        VoiceBean("en_female_dacey_uranus_bigtts", "美式英语", "Dacey", SourceType.VOLCANO),
        VoiceBean("en_female_stokie_uranus_bigtts", "美式英语", "Stokie", SourceType.VOLCANO),
        VoiceBean("zh_female_wenroumama_uranus_bigtts", "中文", "温柔妈妈 2.0", SourceType.VOLCANO),
        VoiceBean("zh_male_jieshuoxiaoming_uranus_bigtts", "中文", "解说小明 2.0", SourceType.VOLCANO)
    )

    private fun getAiuiVoices(): List<VoiceBean> = listOf(
        VoiceBean("x6_lingxiaoyue_pro", "中文", "聆小玥", SourceType.AIUI),
        VoiceBean("x6_lingfeiyi_pro", "中文", "聆飞逸", SourceType.AIUI),
        VoiceBean("x6_lingyuyan_pro", "中文", "聆玉言", SourceType.AIUI),
        VoiceBean("x5_lingxiaotang_flow", "中文", "聆小糖", SourceType.AIUI),
        VoiceBean("x6_lingxiaoxuan_pro", "中文", "聆小璇", SourceType.AIUI),
        VoiceBean("x5_lingyuzhao_flow", "中文", "聆玉昭", SourceType.AIUI),
        VoiceBean("x6_lingxiaoying_pro", "中文", "聆小颖", SourceType.AIUI),
        VoiceBean("x6_lingfeibo_pro", "中文", "聆飞博", SourceType.AIUI),
        VoiceBean("x6_gaolengnanshen_pro", "中文", "高冷男神", SourceType.AIUI),
        VoiceBean("x6_waiguodashu_pro", "中文", "外国人大叔", SourceType.AIUI),
        VoiceBean("x6_dongmanshaonv_pro", "中文", "动漫少女", SourceType.AIUI),
        VoiceBean("x6_wennuancixingnansheng_mini", "中文", "温暖磁性男声", SourceType.AIUI),
        VoiceBean("x6_xiaonaigoudidi_mini", "中文", "小奶狗弟弟", SourceType.AIUI),
        VoiceBean("x6_shibingnvsheng_mini", "中文", "士兵女声", SourceType.AIUI),
        VoiceBean("x6_kongbunvsheng_mini", "中文", "恐怖女声", SourceType.AIUI),
        VoiceBean("x6_yulexinwennvsheng_mini", "中文", "娱乐新闻女声", SourceType.AIUI),
        VoiceBean("x6_wenrounansheng_mini", "中文", "温柔男声", SourceType.AIUI),
        VoiceBean("x6_jingqudaolannvsheng_mini", "中文", "景区导览女声", SourceType.AIUI),
        VoiceBean("x6_daqixuanchuanpiannansheng_mi", "中文", "大气宣传片男声", SourceType.AIUI),
        VoiceBean("x6_cuishounvsheng_pro", "中文", "催收女声", SourceType.AIUI),
        VoiceBean("x6_yingxiaonv_pro", "中文", "营销女声", SourceType.AIUI),
        VoiceBean("x6_huanlemianbao_pro", "中文", "海绵宝宝", SourceType.AIUI),
        VoiceBean("x6_xiangruiyingyu_pro", "中文", "商务殷语", SourceType.AIUI),
        VoiceBean("x6_taiqiangnuannan_pro", "中文", "台湾腔 温柔男声", SourceType.AIUI),
        VoiceBean("x6_wumeinv_pro", "中文", "妩媚姐姐", SourceType.AIUI),
        VoiceBean("x6_lingbosong_pro", "中文", "聆伯松", SourceType.AIUI),
        VoiceBean("x6_dudulibao_pro", "中文", "少女可莉", SourceType.AIUI),
        VoiceBean("x6_huajidama_pro", "中文", "滑稽大妈", SourceType.AIUI),
        VoiceBean("x6_huoposhaonian_pro", "中文", "活泼少年", SourceType.AIUI),
        VoiceBean("x6_lingxiaoxue_pr", "中文", "聆小雪", SourceType.AIUI),
        VoiceBean("x6_gufengxianv_mir", "中文", "古风侠女", SourceType.AIUI),
        VoiceBean("x6_wuyediantai_mini", "中文", "午夜电台", SourceType.AIUI),
        VoiceBean("x6_tiexinnanyou_mini", "中文", "贴心男友", SourceType.AIUI),
        VoiceBean("x6_lingxiaozhen_pro", "中文", "聆小琪", SourceType.AIUI),
        VoiceBean("x6_lingyouyou_pro", "中文", "聆佑佑", SourceType.AIUI),
        VoiceBean("x6_gufengpangbai_pro", "中文", "古风旁白", SourceType.AIUI),
        VoiceBean("x6_ganliannvxing_pro", "中文", "干练女性", SourceType.AIUI),
        VoiceBean("x6_pangbainan1_pro", "中文", "旁白男声", SourceType.AIUI),
        VoiceBean("6_pangbainv1_pro", "中文", "旁白女声", SourceType.AIUI),
        VoiceBean("x6_lingfeihan_pro", "中文", "聆飞瀚", SourceType.AIUI),
        VoiceBean("x6_lingfeihao_pro", "中文", "聆飞皓", SourceType.AIUI),
        VoiceBean("x6_lingyufei_pro", "中文", "聆玉菲", SourceType.AIUI),
        VoiceBean("x6_ruyadashu_pro", "中文", "儒雅大叔", SourceType.AIUI),
        VoiceBean("x6_lingxiaoyun_pro", "中文", "聆小芸", SourceType.AIUI),
        VoiceBean("x6_lingyuaner_pro", "中文", "聆园儿", SourceType.AIUI),
        VoiceBean("x6_lingxiaoshan_pro", "中文", "聆小珊", SourceType.AIUI),
        VoiceBean("x6_feizheChat_pro", "中文", "聆飞哲", SourceType.AIUI),
        VoiceBean("x6_lingxiaoli_pro", "中文", "聆小璃", SourceType.AIUI),
        VoiceBean("x6_xiaoqiChat_pro", "中文", "聆小琪", SourceType.AIUI),
        VoiceBean("x6_youxinanshibing_pro", "中文", "士兵男声", SourceType.AIUI),
        VoiceBean("x6_zhuanyenvzhuchi_pro", "中文", "大会主持女声", SourceType.AIUI),
        VoiceBean("x6_zuixianlibai_pro", "中文", "醉仙李白", SourceType.AIUI),
        VoiceBean("x6_shiwangxiaoxin_pro", "中文", "奶凶辛巴", SourceType.AIUI),
        VoiceBean("x6_bokenansheng_pro", "中文", "播客男声", SourceType.AIUI),
        VoiceBean("x6_zhuanyenanzhuchi_pro", "中文", "大会主持男声", SourceType.AIUI),
        VoiceBean("x6_ranzhinvdazi_pro", "中文", "运动陪练女声", SourceType.AIUI),
        VoiceBean("x6_zhantingnanjiedai_pro", "中文", "展厅接待男声", SourceType.AIUI),
        VoiceBean("x6_zhantingnvjiedai", "中文", "展厅接待女声", SourceType.AIUI),
        VoiceBean("x6_huifangnv_pro", "中文", "回访女声", SourceType.AIUI),
        VoiceBean("x4_lingxiaoqi_oral", "中文", "聆小琪", SourceType.AIUI),
        VoiceBean("x4_lingyuzhao_oral", "中文", "聆玉昭", SourceType.AIUI),
        VoiceBean("x4_lingxiaoxuan_oral", "中文", "聆小璇", SourceType.AIUI),
        VoiceBean("x4_lingfeizhe_oral", "中文", "聆飞哲", SourceType.AIUI),
        VoiceBean("x4_lingyouyou_oral", "中文", "聆佑佑", SourceType.AIUI),
        VoiceBean("x4_lingfeiyi_oral", "中文", "聆飞逸", SourceType.AIUI),
        VoiceBean("x4_lingyuyan_oral", "中文", "聆玉言", SourceType.AIUI),
        VoiceBean("x4_lingxiaoyue_oral", "中文", "聆小玥", SourceType.AIUI),
        VoiceBean("x4_lingxiaoli_oral", "中文", "聆小璃", SourceType.AIUI),
        VoiceBean("x4_zijin_oral", "中文", "子津", SourceType.AIUI),
        VoiceBean("x4_ziyang_oral", "中文", "子阳", SourceType.AIUI),
        VoiceBean("x4_EnUs_Lila_emo", "英文", "Lila", SourceType.AIUI),
        VoiceBean("x4_EnUs_Grant_emo", "英文", "Grant", SourceType.AIUI),
        VoiceBean("x4_lingxiaoying_em_v2", "中文", "聆小樱", SourceType.AIUI),
        VoiceBean("x4_lingxiaoxuan_chat  x2_xiaojuan", "中文", "聆小璇 小娟", SourceType.AIUI),
        VoiceBean("x_dangdang", "中文", "小姐姐", SourceType.AIUI),
        VoiceBean("x_qige", "中文", "小哥哥", SourceType.AIUI),
        VoiceBean("x2_xiaoyuan", "中文", "小媛", SourceType.AIUI),
        VoiceBean("x2 xiaoxi", "中文", "水哥", SourceType.AIUI),
        VoiceBean("x2_chongchong", "中文", "虫虫", SourceType.AIUI),
        VoiceBean("x2_xiaoxue", "中文", "小雪", SourceType.AIUI),
        VoiceBean("x2_pengfei", "中文", "小鹏", SourceType.AIUI),
        VoiceBean("x2_yifei", "中文", "一菲", SourceType.AIUI),
        VoiceBean("x2_chaoge", "中文", "超哥", SourceType.AIUI),
        VoiceBean("x2_qige", "中文", "七哥", SourceType.AIUI),
        VoiceBean("x2_xiaofang", "中文", "芳芳", SourceType.AIUI),
        VoiceBean("x2_nannan", "中文", "楠楠", SourceType.AIUI),
        VoiceBean("x2_wanshu", "中文", "万叔", SourceType.AIUI),
        VoiceBean("x2_xiaofeng", "中文", "晓峰", SourceType.AIUI),
        VoiceBean("x2_xiaozhang", "中文", "刚哥", SourceType.AIUI),
        VoiceBean("x2_yezi", "中文", "小露", SourceType.AIUI),
        VoiceBean("x2_qianqian", "中文", "倩倩", SourceType.AIUI),
        VoiceBean("x2_xiaoshi_cts", "中文", "小师", SourceType.AIUI),
        VoiceBean("x2_xiaoya", "中文", "小雅", SourceType.AIUI),
        VoiceBean("x2_xiaomo", "中文", "小莫", SourceType.AIUI),
        VoiceBean("x2 xiaolan", "中文", "小兰", SourceType.AIUI),
        VoiceBean("x2_john", "英文", "John", SourceType.AIUI),
        VoiceBean("x2_xiaowanzi", "中文", "小桃丸", SourceType.AIUI),
        VoiceBean("x2_xiaoxin", "中文", "萌小新", SourceType.AIUI),
        VoiceBean("x2_ningning", "中文", "宁宁", SourceType.AIUI),
        VoiceBean("x2_qianxue", "中文", "千雪", SourceType.AIUI),
        VoiceBean("x2_xiaoguo", "中文", "小果", SourceType.AIUI),
        VoiceBean("x2 xiaowan", "中文", "小婉", SourceType.AIUI),
        VoiceBean("x2 SuhCn XiXi", "中文", "苏小曦", SourceType.AIUI),
        VoiceBean("x2_xiaoqian", "中文", "晓倩", SourceType.AIUI),
        VoiceBean("x2_xiaorong", "中文", "小蓉", SourceType.AIUI),
        VoiceBean("x2_xiaoying", "中文", "小莹", SourceType.AIUI),
        VoiceBean("x2_xiaoqiang", "中文", "小强", SourceType.AIUI),
        VoiceBean("x2_xiaokun", "中文", "小坤", SourceType.AIUI),
        VoiceBean("x2_xiaodong", "中文", "小东", SourceType.AIUI),
        VoiceBean("x2_xiaowang", "中文", "小王", SourceType.AIUI),
        VoiceBean("x2 xiaohou", "中文", "小侯", SourceType.AIUI),
        VoiceBean("x4_lingxiaowan_boytalk", "中文", "聆万万", SourceType.AIUI),
        VoiceBean("x2 xiaofei", "中文", "小肥", SourceType.AIUI),
        VoiceBean("x2_xiaobao", "中文", "小包", SourceType.AIUI),
        VoiceBean("x2_catherine", "英文", "凯瑟琳", SourceType.AIUI),
        VoiceBean("x2_yifeng", "中文", "一峰", SourceType.AIUI),
        VoiceBean("x2_qianxue2", "中文", "千雪-情感", SourceType.AIUI),
        VoiceBean("x2_yuanye", "中文", "易阳泽 (原野)", SourceType.AIUI),
        VoiceBean("x2_xiaoding", "中文", "小丁", SourceType.AIUI),
        VoiceBean("x_mengmengneutral", "中文", "讯飞萌萌- 中立", SourceType.AIUI),
        VoiceBean("x2 xiaoxuan", "中文", "晓璇", SourceType.AIUI),
        VoiceBean("x2_dahuilang", "中文", "康铭 (大灰狼)", SourceType.AIUI),
        VoiceBean("x2_yiping", "中文", "一萍", SourceType.AIUI),
        VoiceBean("x2_guange", "中文", "管哥", SourceType.AIUI),
        VoiceBean("x2_mingge", "中文", "明哥", SourceType.AIUI),
        VoiceBean("x2_tiange", "中文", "天哥", SourceType.AIUI),
        VoiceBean("x2 chunchun", "中文", "春春", SourceType.AIUI),
        VoiceBean("x2_dangdang", "中文", "当当", SourceType.AIUI),
        VoiceBean("x2_xiaozhong", "中文", "小忠", SourceType.AIUI),
        VoiceBean("x2 xiaoai novel", "中文", "诺诺", SourceType.AIUI),
        VoiceBean("x2_sgron_dkar", "藏语", "达娃", SourceType.AIUI),
        VoiceBean("x2_xiaoyue", "中文", "小月", SourceType.AIUI),
        VoiceBean("x2_abha", "外语", "艾伯哈", SourceType.AIUI),
        VoiceBean("x2_miya", "外语", "miya", SourceType.AIUI),
        VoiceBean("x2_keshu", "外语", "keshu", SourceType.AIUI),
        VoiceBean("x2 Irina", "外语", "Irina", SourceType.AIUI),
        VoiceBean("x2_felisa", "外语", "felisa", SourceType.AIUI),
        VoiceBean("x2 natali", "外语", "natali", SourceType.AIUI),
        VoiceBean("x2 mariane", "外语", "玛丽安", SourceType.AIUI),
        VoiceBean("x2_suparut", "外语", "suparut", SourceType.AIUI),
        VoiceBean("x2_zhongcun", "日语", "zhongcun", SourceType.AIUI),
        VoiceBean("x2_malayrole", "马来语", "malayrole", SourceType.AIUI),
        VoiceBean("x2 christiane", "外语", "christiane", SourceType.AIUI),
        VoiceBean("x2 annakz", "外语", "annakz", SourceType.AIUI),
        VoiceBean("x2 mohamed", "阿拉伯语", "mohamed", SourceType.AIUI),
        VoiceBean("x4_enuk_ashleigh_assist", "英文", "ashleigh", SourceType.AIUI),
        VoiceBean("x4 enuk amanda education", "英文", "amanda", SourceType.AIUI),
        VoiceBean("x4 enus luna assist", "英文", "luna", SourceType.AIUI),
        VoiceBean("x4_enus_gavin_assist", "英文", "gavin", SourceType.AIUI)
    )

    private fun getMicrosoftVoices(): List<VoiceBean> = listOf(
        // af-ZA: 南非荷兰语（南非）
        VoiceBean("af-ZA-AdriNeural", "南非荷兰语（南非）", "女", SourceType.MICROSOFT, "af-ZA"),
        VoiceBean("af-ZA-WillemNeural", "南非荷兰语（南非）", "男", SourceType.MICROSOFT, "af-ZA"),

        // am-ET: 阿姆哈拉语（埃塞俄比亚）
        VoiceBean("am-ET-MekdesNeural", "阿姆哈拉语（埃塞俄比亚）", "女", SourceType.MICROSOFT, "am-ET"),
        VoiceBean("am-ET-AmehaNeural", "阿姆哈拉语（埃塞俄比亚）", "男", SourceType.MICROSOFT, "am-ET"),

        // ar-AE: 阿拉伯语（阿拉伯联合酋长国）
        VoiceBean("ar-AE-FatimaNeural", "阿拉伯语（阿拉伯联合酋长国）", "女", SourceType.MICROSOFT, "ar-AE"),
        VoiceBean("ar-AE-HamdanNeural", "阿拉伯语（阿拉伯联合酋长国）", "男", SourceType.MICROSOFT, "ar-AE"),

        // ar-BH: 阿拉伯语（巴林）
        VoiceBean("ar-BH-LailaNeural", "阿拉伯语（巴林）", "女", SourceType.MICROSOFT, "ar-BH"),
        VoiceBean("ar-BH-AliNeural", "阿拉伯语（巴林）", "男", SourceType.MICROSOFT, "ar-BH"),

        // ar-DZ: 阿拉伯语（阿尔及利亚）
        VoiceBean("ar-DZ-AminaNeural", "阿拉伯语（阿尔及利亚）", "女", SourceType.MICROSOFT, "ar-DZ"),
        VoiceBean("ar-DZ-IsmaelNeural", "阿拉伯语（阿尔及利亚）", "男", SourceType.MICROSOFT, "ar-DZ"),

        // ar-EG: 阿拉伯语（埃及）
        VoiceBean("ar-EG-SalmaNeural", "阿拉伯语（埃及）", "女", SourceType.MICROSOFT, "ar-EG"),
        VoiceBean("ar-EG-ShakirNeural", "阿拉伯语（埃及）", "男", SourceType.MICROSOFT, "ar-EG"),

        // ar-IQ: 阿拉伯语（伊拉克）
        VoiceBean("ar-IQ-RanaNeural", "阿拉伯语（伊拉克）", "女", SourceType.MICROSOFT, "ar-IQ"),
        VoiceBean("ar-IQ-BasselNeural", "阿拉伯语（伊拉克）", "男", SourceType.MICROSOFT, "ar-IQ"),

        // ar-JO: 阿拉伯语（约旦）
        VoiceBean("ar-JO-SanaNeural", "阿拉伯语（约旦）", "女", SourceType.MICROSOFT, "ar-JO"),
        VoiceBean("ar-JO-TaimNeural", "阿拉伯语（约旦）", "男", SourceType.MICROSOFT, "ar-JO"),

        // ar-KW: 阿拉伯语（科威特）
        VoiceBean("ar-KW-NouraNeural", "阿拉伯语（科威特）", "女", SourceType.MICROSOFT, "ar-KW"),
        VoiceBean("ar-KW-FahedNeural", "阿拉伯语（科威特）", "男", SourceType.MICROSOFT, "ar-KW"),

        // ar-LB: 阿拉伯语（黎巴嫩）
        VoiceBean("ar-LB-LaylaNeural", "阿拉伯语（黎巴嫩）", "女", SourceType.MICROSOFT, "ar-LB"),
        VoiceBean("ar-LB-RamiNeural", "阿拉伯语（黎巴嫩）", "男", SourceType.MICROSOFT, "ar-LB"),

        // ar-LY: 阿拉伯语（利比亚）
        VoiceBean("ar-LY-ImanNeural", "阿拉伯语（利比亚）", "女", SourceType.MICROSOFT, "ar-LY"),
        VoiceBean("ar-LY-OmarNeural", "阿拉伯语（利比亚）", "男", SourceType.MICROSOFT, "ar-LY"),

        // ar-MA: 阿拉伯语（摩洛哥）
        VoiceBean("ar-MA-MounaNeural", "阿拉伯语（摩洛哥）", "女", SourceType.MICROSOFT, "ar-MA"),
        VoiceBean("ar-MA-JamalNeural", "阿拉伯语（摩洛哥）", "男", SourceType.MICROSOFT, "ar-MA"),

        // ar-OM: 阿拉伯语（阿曼）
        VoiceBean("ar-OM-AyshaNeural", "阿拉伯语（阿曼）", "女", SourceType.MICROSOFT, "ar-OM"),
        VoiceBean("ar-OM-AbdullahNeural", "阿拉伯语（阿曼）", "男", SourceType.MICROSOFT, "ar-OM"),

        // ar-QA: 阿拉伯语（卡塔尔）
        VoiceBean("ar-QA-AmalNeural", "阿拉伯语（卡塔尔）", "女", SourceType.MICROSOFT, "ar-QA"),
        VoiceBean("ar-QA-MoazNeural", "阿拉伯语（卡塔尔）", "男", SourceType.MICROSOFT, "ar-QA"),

        // ar-SA: 阿拉伯语（沙特阿拉伯）
        VoiceBean("ar-SA-ZariyahNeural", "阿拉伯语（沙特阿拉伯）", "女", SourceType.MICROSOFT, "ar-SA"),
        VoiceBean("ar-SA-HamedNeural", "阿拉伯语（沙特阿拉伯）", "男", SourceType.MICROSOFT, "ar-SA"),

        // ar-SY: 阿拉伯语（叙利亚）
        VoiceBean("ar-SY-AmanyNeural", "阿拉伯语（叙利亚）", "女", SourceType.MICROSOFT, "ar-SY"),
        VoiceBean("ar-SY-LaithNeural", "阿拉伯语（叙利亚）", "男", SourceType.MICROSOFT, "ar-SY"),

        // ar-TN: 阿拉伯语（突尼斯）
        VoiceBean("ar-TN-ReemNeural", "阿拉伯语（突尼斯）", "女", SourceType.MICROSOFT, "ar-TN"),
        VoiceBean("ar-TN-HediNeural", "阿拉伯语（突尼斯）", "男", SourceType.MICROSOFT, "ar-TN"),

        // ar-YE: 阿拉伯语（也门）
        VoiceBean("ar-YE-MaryamNeural", "阿拉伯语（也门）", "女", SourceType.MICROSOFT, "ar-YE"),
        VoiceBean("ar-YE-SalehNeural", "阿拉伯语（也门）", "男", SourceType.MICROSOFT, "ar-YE"),

        // as-IN: 阿萨姆语（印度）
        VoiceBean("as-IN-YashicaNeural", "阿萨姆语（印度）", "女", SourceType.MICROSOFT, "as-IN"),
        VoiceBean("as-IN-PriyomNeural", "阿萨姆语（印度）", "男", SourceType.MICROSOFT, "as-IN"),

        // az-AZ: 阿塞拜疆语（拉丁语，阿塞拜疆）
        VoiceBean("az-AZ-BanuNeural", "阿塞拜疆语（拉丁语，阿塞拜疆）", "女", SourceType.MICROSOFT, "az-AZ"),
        VoiceBean("az-AZ-BabekNeural", "阿塞拜疆语（拉丁语，阿塞拜疆）", "男", SourceType.MICROSOFT, "az-AZ"),

        // bg-BG: 保加利亚语（保加利亚）
        VoiceBean("bg-BG-KalinaNeural", "保加利亚语（保加利亚）", "女", SourceType.MICROSOFT, "bg-BG"),
        VoiceBean("bg-BG-BorislavNeural", "保加利亚语（保加利亚）", "男", SourceType.MICROSOFT, "bg-BG"),

        // bn-BD: 孟加拉语（孟加拉国）
        VoiceBean("bn-BD-NabanitaNeural", "孟加拉语（孟加拉国）", "女", SourceType.MICROSOFT, "bn-BD"),
        VoiceBean("bn-BD-PradeepNeural", "孟加拉语（孟加拉国）", "男", SourceType.MICROSOFT, "bn-BD"),

        // bn-IN: 孟加拉语（印度）
        VoiceBean("bn-IN-TanishaaNeural", "孟加拉语（印度）", "女", SourceType.MICROSOFT, "bn-IN"),
        VoiceBean("bn-IN-BashkarNeural", "孟加拉语（印度）", "男", SourceType.MICROSOFT, "bn-IN"),

        // bs-BA: 波斯尼亚语（波斯尼亚和黑塞哥维那）
        VoiceBean("bs-BA-VesnaNeural", "波斯尼亚语（波斯尼亚和黑塞哥维那）", "女", SourceType.MICROSOFT, "bs-BA"),
        VoiceBean("bs-BA-GoranNeural", "波斯尼亚语（波斯尼亚和黑塞哥维那）", "男", SourceType.MICROSOFT, "bs-BA"),

        // ca-ES: 加泰罗尼亚语
        VoiceBean("ca-ES-JoanaNeural", "加泰罗尼亚语", "女", SourceType.MICROSOFT, "ca-ES"),
        VoiceBean("ca-ES-EnricNeural", "加泰罗尼亚语", "男", SourceType.MICROSOFT, "ca-ES"),
        VoiceBean("ca-ES-AlbaNeural", "加泰罗尼亚语", "女", SourceType.MICROSOFT, "ca-ES"),

        // cs-CZ: 捷克语(捷克)
        VoiceBean("cs-CZ-VlastaNeural", "捷克语(捷克)", "女", SourceType.MICROSOFT, "cs-CZ"),
        VoiceBean("cs-CZ-AntoninNeural", "捷克语(捷克)", "男", SourceType.MICROSOFT, "cs-CZ"),

        // cy-GB: 威尔士语（英国）
        VoiceBean("cy-GB-NiaNeural", "威尔士语（英国）", "女", SourceType.MICROSOFT, "cy-GB"),
        VoiceBean("cy-GB-AledNeural", "威尔士语（英国）", "男", SourceType.MICROSOFT, "cy-GB"),

        // da-DK: 丹麦语（丹麦）
        VoiceBean("da-DK-ChristelNeural", "丹麦语（丹麦）", "女", SourceType.MICROSOFT, "da-DK"),
        VoiceBean("da-DK-JeppeNeural", "丹麦语（丹麦）", "男", SourceType.MICROSOFT, "da-DK"),

        // de-AT: 德语（奥地利）
        VoiceBean("de-AT-IngridNeural", "德语（奥地利）", "女", SourceType.MICROSOFT, "de-AT"),
        VoiceBean("de-AT-JonasNeural", "德语（奥地利）", "男", SourceType.MICROSOFT, "de-AT"),

        // de-CH: 德语（瑞士）
        VoiceBean("de-CH-LeniNeural", "德语（瑞士）", "女", SourceType.MICROSOFT, "de-CH"),
        VoiceBean("de-CH-JanNeural", "德语（瑞士）", "男", SourceType.MICROSOFT, "de-CH"),

        // de-DE: 德语（德国）
        VoiceBean("de-DE-KatjaNeural", "德语（德国）", "女", SourceType.MICROSOFT, "de-DE"),
        VoiceBean("de-DE-ConradNeural", "德语（德国）", "男", SourceType.MICROSOFT, "de-DE"),
        VoiceBean("de-DE-SeraphinaMultilingualNeural", "德语（德国）", "女", SourceType.MICROSOFT, "de-DE"),
        VoiceBean("de-DE-FlorianMultilingualNeural", "德语（德国）", "男", SourceType.MICROSOFT, "de-DE"),
        VoiceBean("de-DE-AmalaNeural", "德语（德国）", "女", SourceType.MICROSOFT, "de-DE"),
        VoiceBean("de-DE-BerndNeural", "德语（德国）", "男", SourceType.MICROSOFT, "de-DE"),
        VoiceBean("de-DE-ChristophNeural", "德语（德国）", "男", SourceType.MICROSOFT, "de-DE"),
        VoiceBean("de-DE-ElkeNeural", "德语（德国）", "女", SourceType.MICROSOFT, "de-DE"),
        VoiceBean("de-DE-GiselaNeural", "德语（德国）", "中性", SourceType.MICROSOFT, "de-DE"),
        VoiceBean("de-DE-KasperNeural", "德语（德国）", "男", SourceType.MICROSOFT, "de-DE"),
        VoiceBean("de-DE-KillianNeural", "德语（德国）", "男", SourceType.MICROSOFT, "de-DE"),
        VoiceBean("de-DE-KlarissaNeural", "德语（德国）", "女", SourceType.MICROSOFT, "de-DE"),
        VoiceBean("de-DE-KlausNeural", "德语（德国）", "男", SourceType.MICROSOFT, "de-DE"),
        VoiceBean("de-DE-LouisaNeural", "德语（德国）", "女", SourceType.MICROSOFT, "de-DE"),
        VoiceBean("de-DE-MajaNeural", "德语（德国）", "女", SourceType.MICROSOFT, "de-DE"),
        VoiceBean("de-DE-RalfNeural", "德语（德国）", "男", SourceType.MICROSOFT, "de-DE"),
        VoiceBean("de-DE-TanjaNeural", "德语（德国）", "女", SourceType.MICROSOFT, "de-DE"),
        VoiceBean("de-DE-Florian:DragonHDLatestNeural", "德语（德国）", "男", SourceType.MICROSOFT, "de-DE"),
        VoiceBean("de-DE-Seraphina:DragonHDLatestNeural", "德语（德国）", "女", SourceType.MICROSOFT, "de-DE"),

        // el-GR: 希腊语（希腊）
        VoiceBean("el-GR-AthinaNeural", "希腊语（希腊）", "女", SourceType.MICROSOFT, "el-GR"),
        VoiceBean("el-GR-NestorasNeural", "希腊语（希腊）", "男", SourceType.MICROSOFT, "el-GR"),

        // en-AU: 英语（澳大利亚）
        VoiceBean("en-AU-NatashaNeural", "英语（澳大利亚）", "女", SourceType.MICROSOFT, "en-AU"),
        VoiceBean("en-AU-WilliamNeural", "英语（澳大利亚）", "男", SourceType.MICROSOFT, "en-AU"),
        VoiceBean("en-AU-WilliamMultilingualNeural", "英语（澳大利亚）", "男", SourceType.MICROSOFT, "en-AU"),
        VoiceBean("en-AU-AnnetteNeural", "英语（澳大利亚）", "女", SourceType.MICROSOFT, "en-AU"),
        VoiceBean("en-AU-CarlyNeural", "英语（澳大利亚）", "女", SourceType.MICROSOFT, "en-AU"),
        VoiceBean("en-AU-DarrenNeural", "英语（澳大利亚）", "男", SourceType.MICROSOFT, "en-AU"),
        VoiceBean("en-AU-DuncanNeural", "英语（澳大利亚）", "男", SourceType.MICROSOFT, "en-AU"),
        VoiceBean("en-AU-ElsieNeural", "英语（澳大利亚）", "女", SourceType.MICROSOFT, "en-AU"),
        VoiceBean("en-AU-FreyaNeural", "英语（澳大利亚）", "女", SourceType.MICROSOFT, "en-AU"),
        VoiceBean("en-AU-JoanneNeural", "英语（澳大利亚）", "女", SourceType.MICROSOFT, "en-AU"),
        VoiceBean("en-AU-KenNeural", "英语（澳大利亚）", "男", SourceType.MICROSOFT, "en-AU"),
        VoiceBean("en-AU-KimNeural", "英语（澳大利亚）", "女", SourceType.MICROSOFT, "en-AU"),
        VoiceBean("en-AU-NeilNeural", "英语（澳大利亚）", "男", SourceType.MICROSOFT, "en-AU"),
        VoiceBean("en-AU-TimNeural", "英语（澳大利亚）", "男", SourceType.MICROSOFT, "en-AU"),
        VoiceBean("en-AU-TinaNeural", "英语（澳大利亚）", "女", SourceType.MICROSOFT, "en-AU"),

        // en-CA: 英语（加拿大）
        VoiceBean("en-CA-ClaraNeural", "英语（加拿大）", "女", SourceType.MICROSOFT, "en-CA"),
        VoiceBean("en-CA-LiamNeural", "英语（加拿大）", "男", SourceType.MICROSOFT, "en-CA"),

        // en-GB: 英语（英国）
        VoiceBean("en-GB-SoniaNeural", "英语（英国）", "女", SourceType.MICROSOFT, "en-GB"),
        VoiceBean("en-GB-RyanNeural", "英语（英国）", "男", SourceType.MICROSOFT, "en-GB"),
        VoiceBean("en-GB-LibbyNeural", "英语（英国）", "女", SourceType.MICROSOFT, "en-GB"),
        VoiceBean("en-GB-AdaMultilingualNeural", "英语（英国）", "女", SourceType.MICROSOFT, "en-GB"),
        VoiceBean("en-GB-OllieMultilingualNeural", "英语（英国）", "男", SourceType.MICROSOFT, "en-GB"),
        VoiceBean("en-GB-AbbiNeural", "英语（英国）", "女", SourceType.MICROSOFT, "en-GB"),
        VoiceBean("en-GB-AlfieNeural", "英语（英国）", "男", SourceType.MICROSOFT, "en-GB"),
        VoiceBean("en-GB-BellaNeural", "英语（英国）", "女", SourceType.MICROSOFT, "en-GB"),
        VoiceBean("en-GB-ElliotNeural", "英语（英国）", "男", SourceType.MICROSOFT, "en-GB"),
        VoiceBean("en-GB-EthanNeural", "英语（英国）", "男", SourceType.MICROSOFT, "en-GB"),
        VoiceBean("en-GB-HollieNeural", "英语（英国）", "女", SourceType.MICROSOFT, "en-GB"),
        VoiceBean("en-GB-MaisieNeural", "英语（英国）", "中性", SourceType.MICROSOFT, "en-GB"),
        VoiceBean("en-GB-NoahNeural", "英语（英国）", "男", SourceType.MICROSOFT, "en-GB"),
        VoiceBean("en-GB-OliverNeural", "英语（英国）", "男", SourceType.MICROSOFT, "en-GB"),
        VoiceBean("en-GB-OliviaNeural", "英语（英国）", "女", SourceType.MICROSOFT, "en-GB"),
        VoiceBean("en-GB-ThomasNeural", "英语（英国）", "男", SourceType.MICROSOFT, "en-GB"),

        // en-HK: 英语（香港特別行政区）
        VoiceBean("en-HK-YanNeural", "英语（香港特別行政区）", "女", SourceType.MICROSOFT, "en-HK"),
        VoiceBean("en-HK-SamNeural", "英语（香港特別行政区）", "男", SourceType.MICROSOFT, "en-HK"),

        // en-IE: 英语（爱尔兰）
        VoiceBean("en-IE-EmilyNeural", "英语（爱尔兰）", "女", SourceType.MICROSOFT, "en-IE"),
        VoiceBean("en-IE-ConnorNeural", "英语（爱尔兰）", "男", SourceType.MICROSOFT, "en-IE"),

        // en-IN: 英语（印度）
        VoiceBean("en-IN-AartiIndicNeural", "英语（印度）", "女", SourceType.MICROSOFT, "en-IN"),
        VoiceBean("en-IN-ArjunIndicNeural", "英语（印度）", "男", SourceType.MICROSOFT, "en-IN"),
        VoiceBean("en-IN-NeerjaIndicNeural", "英语（印度）", "女", SourceType.MICROSOFT, "en-IN"),
        VoiceBean("en-IN-PrabhatIndicNeural", "英语（印度）", "男", SourceType.MICROSOFT, "en-IN"),
        VoiceBean("en-IN-AaravNeural", "英语（印度）", "男", SourceType.MICROSOFT, "en-IN"),
        VoiceBean("en-IN-AashiNeural", "英语（印度）", "女", SourceType.MICROSOFT, "en-IN"),
        VoiceBean("en-IN-AartiNeural", "英语（印度）", "女", SourceType.MICROSOFT, "en-IN"),
        VoiceBean("en-IN-ArjunNeural", "英语（印度）", "男", SourceType.MICROSOFT, "en-IN"),
        VoiceBean("en-IN-AnanyaNeural", "英语（印度）", "女", SourceType.MICROSOFT, "en-IN"),
        VoiceBean("en-IN-KavyaNeural", "英语（印度）", "女", SourceType.MICROSOFT, "en-IN"),
        VoiceBean("en-IN-KunalNeural", "英语（印度）", "男", SourceType.MICROSOFT, "en-IN"),
        VoiceBean("en-IN-NeerjaNeural", "英语（印度）", "女", SourceType.MICROSOFT, "en-IN"),
        VoiceBean("en-IN-PrabhatNeural", "英语（印度）", "男", SourceType.MICROSOFT, "en-IN"),
        VoiceBean("en-IN-RehaanNeural", "英语（印度）", "男", SourceType.MICROSOFT, "en-IN"),

        // en-KE: 英语（肯尼亚）
        VoiceBean("en-KE-AsiliaNeural", "英语（肯尼亚）", "女", SourceType.MICROSOFT, "en-KE"),
        VoiceBean("en-KE-ChilembaNeural", "英语（肯尼亚）", "男", SourceType.MICROSOFT, "en-KE"),

        // en-NG: 英语（尼日利亚）
        VoiceBean("en-NG-EzinneNeural", "英语（尼日利亚）", "女", SourceType.MICROSOFT, "en-NG"),
        VoiceBean("en-NG-AbeoNeural", "英语（尼日利亚）", "男", SourceType.MICROSOFT, "en-NG"),

        // en-NZ: 英语（新西兰）
        VoiceBean("en-NZ-MollyNeural", "英语（新西兰）", "女", SourceType.MICROSOFT, "en-NZ"),
        VoiceBean("en-NZ-MitchellNeural", "英语（新西兰）", "男", SourceType.MICROSOFT, "en-NZ"),

        // en-PH: 英语（菲律宾）
        VoiceBean("en-PH-RosaNeural", "英语（菲律宾）", "女", SourceType.MICROSOFT, "en-PH"),
        VoiceBean("en-PH-JamesNeural", "英语（菲律宾）", "男", SourceType.MICROSOFT, "en-PH"),

        // en-SG: 英语（新加坡）
        VoiceBean("en-SG-LunaNeural", "英语（新加坡）", "女", SourceType.MICROSOFT, "en-SG"),
        VoiceBean("en-SG-WayneNeural", "英语（新加坡）", "男", SourceType.MICROSOFT, "en-SG"),

        // en-TZ: 英语（坦桑尼亚）
        VoiceBean("en-TZ-ImaniNeural", "英语（坦桑尼亚）", "女", SourceType.MICROSOFT, "en-TZ"),
        VoiceBean("en-TZ-ElimuNeural", "英语（坦桑尼亚）", "男", SourceType.MICROSOFT, "en-TZ"),

        // en-US: 英语（美国）
        VoiceBean("en-US-AvaMultilingualNeural", "英语（美国）", "女", SourceType.MICROSOFT, "en-US"),
        VoiceBean("en-US-AndrewMultilingualNeural", "英语（美国）", "男", SourceType.MICROSOFT, "en-US"),
        VoiceBean("en-US-EmmaMultilingualNeural", "英语（美国）", "女", SourceType.MICROSOFT, "en-US"),
        VoiceBean("en-US-AlloyTurboMultilingualNeural", "英语（美国）", "男", SourceType.MICROSOFT, "en-US"),
        VoiceBean("en-US-EchoTurboMultilingualNeural", "英语（美国）", "男", SourceType.MICROSOFT, "en-US"),
        VoiceBean("en-US-FableTurboMultilingualNeural", "英语（美国）", "中性", SourceType.MICROSOFT, "en-US"),
        VoiceBean("en-US-OnyxTurboMultilingualNeural", "英语（美国）", "男", SourceType.MICROSOFT, "en-US"),
        VoiceBean("en-US-NovaTurboMultilingualNeural", "英语（美国）", "女", SourceType.MICROSOFT, "en-US"),
        VoiceBean("en-US-ShimmerTurboMultilingualNeural", "英语（美国）", "女", SourceType.MICROSOFT, "en-US"),
        VoiceBean("en-US-BrianMultilingualNeural", "英语（美国）", "男", SourceType.MICROSOFT, "en-US"),
        VoiceBean("en-US-AvaNeural", "英语（美国）", "女", SourceType.MICROSOFT, "en-US"),
        VoiceBean("en-US-AndrewNeural", "英语（美国）", "男", SourceType.MICROSOFT, "en-US"),
        VoiceBean("en-US-EmmaNeural", "英语（美国）", "女", SourceType.MICROSOFT, "en-US"),
        VoiceBean("en-US-BrianNeural", "英语（美国）", "男", SourceType.MICROSOFT, "en-US"),
        VoiceBean("en-US-JennyNeural", "英语（美国）", "女", SourceType.MICROSOFT, "en-US"),
        VoiceBean("en-US-GuyNeural", "英语（美国）", "男", SourceType.MICROSOFT, "en-US"),
        VoiceBean("en-US-AriaNeural", "英语（美国）", "女", SourceType.MICROSOFT, "en-US"),
        VoiceBean("en-US-DavisNeural", "英语（美国）", "男", SourceType.MICROSOFT, "en-US"),
        VoiceBean("en-US-JaneNeural", "英语（美国）", "女", SourceType.MICROSOFT, "en-US"),
        VoiceBean("en-US-JasonNeural", "英语（美国）", "男", SourceType.MICROSOFT, "en-US"),
        VoiceBean("en-US-KaiNeural", "英语（美国）", "男", SourceType.MICROSOFT, "en-US"),
        VoiceBean("en-US-LunaNeural", "英语（美国）", "女", SourceType.MICROSOFT, "en-US"),
        VoiceBean("en-US-SaraNeural", "英语（美国）", "女", SourceType.MICROSOFT, "en-US"),
        VoiceBean("en-US-TonyNeural", "英语（美国）", "男", SourceType.MICROSOFT, "en-US"),
        VoiceBean("en-US-NancyNeural", "英语（美国）", "女", SourceType.MICROSOFT, "en-US"),
        VoiceBean("en-US-CoraMultilingualNeural", "英语（美国）", "女", SourceType.MICROSOFT, "en-US"),
        VoiceBean("en-US-ChristopherMultilingualNeural", "英语（美国）", "男", SourceType.MICROSOFT, "en-US"),
        VoiceBean("en-US-BrandonMultilingualNeural", "英语（美国）", "男", SourceType.MICROSOFT, "en-US"),
        VoiceBean("en-US-AmberNeural", "英语（美国）", "女", SourceType.MICROSOFT, "en-US"),
        VoiceBean("en-US-AnaNeural", "英语（美国）", "中性", SourceType.MICROSOFT, "en-US"),
        VoiceBean("en-US-AshleyNeural", "英语（美国）", "女", SourceType.MICROSOFT, "en-US"),
        VoiceBean("en-US-BrandonNeural", "英语（美国）", "男", SourceType.MICROSOFT, "en-US"),
        VoiceBean("en-US-ChristopherNeural", "英语（美国）", "男", SourceType.MICROSOFT, "en-US"),
        VoiceBean("en-US-CoraNeural", "英语（美国）", "女", SourceType.MICROSOFT, "en-US"),
        VoiceBean("en-US-ElizabethNeural", "英语（美国）", "女", SourceType.MICROSOFT, "en-US"),
        VoiceBean("en-US-EricNeural", "英语（美国）", "男", SourceType.MICROSOFT, "en-US"),
        VoiceBean("en-US-JacobNeural", "英语（美国）", "男", SourceType.MICROSOFT, "en-US"),
        VoiceBean("en-US-JennyMultilingualNeural", "英语（美国）", "女", SourceType.MICROSOFT, "en-US"),
        VoiceBean("en-US-MichelleNeural", "英语（美国）", "女", SourceType.MICROSOFT, "en-US"),
        VoiceBean("en-US-MonicaNeural", "英语（美国）", "女", SourceType.MICROSOFT, "en-US"),
        VoiceBean("en-US-RogerNeural", "英语（美国）", "男", SourceType.MICROSOFT, "en-US"),
        VoiceBean("en-US-RyanMultilingualNeural", "英语（美国）", "男", SourceType.MICROSOFT, "en-US"),
        VoiceBean("en-US-SteffanMultilingualNeural", "英语（美国）", "男", SourceType.MICROSOFT, "en-US"),
        VoiceBean("en-US-SteffanNeural", "英语（美国）", "男", SourceType.MICROSOFT, "en-US"),
        VoiceBean("en-US-AdamMultilingualNeural", "英语（美国）", "男", SourceType.MICROSOFT, "en-US"),
        VoiceBean("en-US-AIGenerate1Neural", "英语（美国）", "男", SourceType.MICROSOFT, "en-US"),
        VoiceBean("en-US-AIGenerate2Neural", "英语（美国）", "女", SourceType.MICROSOFT, "en-US"),
        VoiceBean("en-US-AmandaMultilingualNeural", "英语（美国）", "女", SourceType.MICROSOFT, "en-US"),
        VoiceBean("en-US-AshTurboMultilingualNeural", "英语（美国）", "男", SourceType.MICROSOFT, "en-US"),
        VoiceBean("en-US-BlueNeural", "英语（美国）", "中性", SourceType.MICROSOFT, "en-US"),
        VoiceBean("en-US-DavisMultilingualNeural", "英语（美国）", "男", SourceType.MICROSOFT, "en-US"),
        VoiceBean("en-US-DerekMultilingualNeural", "英语（美国）", "男", SourceType.MICROSOFT, "en-US"),
        VoiceBean("en-US-DustinMultilingualNeural", "英语（美国）", "男", SourceType.MICROSOFT, "en-US"),
        VoiceBean("en-US-EvelynMultilingualNeural", "英语（美国）", "女", SourceType.MICROSOFT, "en-US"),
        VoiceBean("en-US-LewisMultilingualNeural", "英语（美国）", "男", SourceType.MICROSOFT, "en-US"),
        VoiceBean("en-US-LolaMultilingualNeural", "英语（美国）", "女", SourceType.MICROSOFT, "en-US"),
        VoiceBean("en-US-NancyMultilingualNeural", "英语（美国）", "女", SourceType.MICROSOFT, "en-US"),
        VoiceBean("en-US-PhoebeMultilingualNeural", "英语（美国）", "女", SourceType.MICROSOFT, "en-US"),
        VoiceBean("en-US-SamuelMultilingualNeural", "英语（美国）", "男", SourceType.MICROSOFT, "en-US"),
        VoiceBean("en-US-SerenaMultilingualNeural", "英语（美国）", "女", SourceType.MICROSOFT, "en-US"),
        VoiceBean("en-US-Adam:DragonHDLatestNeural", "英语（美国）", "男", SourceType.MICROSOFT, "en-US"),
        VoiceBean("en-US-Andrew:DragonHDLatestNeural", "英语（美国）", "男", SourceType.MICROSOFT, "en-US"),
        VoiceBean("en-US-Andrew2:DragonHDLatestNeural", "英语（美国）", "男", SourceType.MICROSOFT, "en-US"),
        VoiceBean("en-US-Ava:DragonHDLatestNeural", "英语（美国）", "女", SourceType.MICROSOFT, "en-US"),
        VoiceBean("en-US-Brian:DragonHDLatestNeural", "英语（美国）", "男", SourceType.MICROSOFT, "en-US"),
        VoiceBean("en-US-Davis:DragonHDLatestNeural", "英语（美国）", "男", SourceType.MICROSOFT, "en-US"),
        VoiceBean("en-US-Emma:DragonHDLatestNeural", "英语（美国）", "女", SourceType.MICROSOFT, "en-US"),
        VoiceBean("en-US-Emma2:DragonHDLatestNeural", "英语（美国）", "女", SourceType.MICROSOFT, "en-US"),
        VoiceBean("en-US-Steffan:DragonHDLatestNeural", "英语（美国）", "男", SourceType.MICROSOFT, "en-US"),
        VoiceBean("en-US-Alloy:DragonHDLatestNeural", "英语（美国）", "男", SourceType.MICROSOFT, "en-US"),
        VoiceBean("en-US-Andrew3:DragonHDLatestNeural", "英语（美国）", "男", SourceType.MICROSOFT, "en-US"),
        VoiceBean("en-US-Aria:DragonHDLatestNeural", "英语（美国）", "女", SourceType.MICROSOFT, "en-US"),
        VoiceBean("en-US-Ava3:DragonHDLatestNeural", "英语（美国）", "女", SourceType.MICROSOFT, "en-US"),
        VoiceBean("en-US-Bree:DragonHDLatestNeural", "英语（美国）", "女", SourceType.MICROSOFT, "en-US"),
        VoiceBean("en-US-Jane:DragonHDLatestNeural", "英语（美国）", "女", SourceType.MICROSOFT, "en-US"),
        VoiceBean("en-US-Jenny:DragonHDLatestNeural", "英语（美国）", "女", SourceType.MICROSOFT, "en-US"),
        VoiceBean("en-US-MultiTalker-Ava-Andrew:DragonHDLatestNeural", "英语（美国）", "中性", SourceType.MICROSOFT, "en-US"),
        VoiceBean("en-US-MultiTalker-Ava-Steffan:DragonHDLatestNeural", "英语（美国）", "中性", SourceType.MICROSOFT, "en-US"),
        VoiceBean("en-US-Nova:DragonHDLatestNeural", "英语（美国）", "女", SourceType.MICROSOFT, "en-US"),
        VoiceBean("en-US-Phoebe:DragonHDLatestNeural", "英语（美国）", "女", SourceType.MICROSOFT, "en-US"),
        VoiceBean("en-US-Serena:DragonHDLatestNeural", "英语（美国）", "女", SourceType.MICROSOFT, "en-US"),

        // en-ZA: 英语（南非）
        VoiceBean("en-ZA-LeahNeural", "英语（南非）", "女", SourceType.MICROSOFT, "en-ZA"),
        VoiceBean("en-ZA-LukeNeural", "英语（南非）", "男", SourceType.MICROSOFT, "en-ZA"),

        // es-AR: 西班牙语（阿根廷）
        VoiceBean("es-AR-ElenaNeural", "西班牙语（阿根廷）", "女", SourceType.MICROSOFT, "es-AR"),
        VoiceBean("es-AR-TomasNeural", "西班牙语（阿根廷）", "男", SourceType.MICROSOFT, "es-AR"),

        // es-BO: 西班牙语（玻利维亚）
        VoiceBean("es-BO-SofiaNeural", "西班牙语（玻利维亚）", "女", SourceType.MICROSOFT, "es-BO"),
        VoiceBean("es-BO-MarceloNeural", "西班牙语（玻利维亚）", "男", SourceType.MICROSOFT, "es-BO"),

        // es-CL: 西班牙语（智利）
        VoiceBean("es-CL-CatalinaNeural", "西班牙语（智利）", "女", SourceType.MICROSOFT, "es-CL"),
        VoiceBean("es-CL-LorenzoNeural", "西班牙语（智利）", "男", SourceType.MICROSOFT, "es-CL"),

        // es-CO: 西班牙语（哥伦比亚）
        VoiceBean("es-CO-SalomeNeural", "西班牙语（哥伦比亚）", "女", SourceType.MICROSOFT, "es-CO"),
        VoiceBean("es-CO-GonzaloNeural", "西班牙语（哥伦比亚）", "男", SourceType.MICROSOFT, "es-CO"),

        // es-CR: 西班牙语（哥斯达黎加）
        VoiceBean("es-CR-MariaNeural", "西班牙语（哥斯达黎加）", "女", SourceType.MICROSOFT, "es-CR"),
        VoiceBean("es-CR-JuanNeural", "西班牙语（哥斯达黎加）", "男", SourceType.MICROSOFT, "es-CR"),

        // es-CU: 西班牙语（古巴）
        VoiceBean("es-CU-BelkysNeural", "西班牙语（古巴）", "女", SourceType.MICROSOFT, "es-CU"),
        VoiceBean("es-CU-ManuelNeural", "西班牙语（古巴）", "男", SourceType.MICROSOFT, "es-CU"),

        // es-DO: 西班牙语（多米尼加共和国）
        VoiceBean("es-DO-RamonaNeural", "西班牙语（多米尼加共和国）", "女", SourceType.MICROSOFT, "es-DO"),
        VoiceBean("es-DO-EmilioNeural", "西班牙语（多米尼加共和国）", "男", SourceType.MICROSOFT, "es-DO"),

        // es-EC: 西班牙语（厄瓜多尔）
        VoiceBean("es-EC-AndreaNeural", "西班牙语（厄瓜多尔）", "女", SourceType.MICROSOFT, "es-EC"),
        VoiceBean("es-EC-LuisNeural", "西班牙语（厄瓜多尔）", "男", SourceType.MICROSOFT, "es-EC"),

        // es-ES: 西班牙语(西班牙)
        VoiceBean("es-ES-ElviraNeural", "西班牙语(西班牙)", "女", SourceType.MICROSOFT, "es-ES"),
        VoiceBean("es-ES-AlvaroNeural", "西班牙语(西班牙)", "男", SourceType.MICROSOFT, "es-ES"),
        VoiceBean("es-ES-ArabellaMultilingualNeural", "西班牙语(西班牙)", "女", SourceType.MICROSOFT, "es-ES"),
        VoiceBean("es-ES-IsidoraMultilingualNeural", "西班牙语(西班牙)", "女", SourceType.MICROSOFT, "es-ES"),
        VoiceBean("es-ES-TristanMultilingualNeural", "西班牙语(西班牙)", "男", SourceType.MICROSOFT, "es-ES"),
        VoiceBean("es-ES-XimenaMultilingualNeural", "西班牙语(西班牙)", "女", SourceType.MICROSOFT, "es-ES"),
        VoiceBean("es-ES-AbrilNeural", "西班牙语(西班牙)", "女", SourceType.MICROSOFT, "es-ES"),
        VoiceBean("es-ES-ArnauNeural", "西班牙语(西班牙)", "男", SourceType.MICROSOFT, "es-ES"),
        VoiceBean("es-ES-DarioNeural", "西班牙语(西班牙)", "男", SourceType.MICROSOFT, "es-ES"),
        VoiceBean("es-ES-EliasNeural", "西班牙语(西班牙)", "男", SourceType.MICROSOFT, "es-ES"),
        VoiceBean("es-ES-EstrellaNeural", "西班牙语(西班牙)", "女", SourceType.MICROSOFT, "es-ES"),
        VoiceBean("es-ES-IreneNeural", "西班牙语(西班牙)", "女", SourceType.MICROSOFT, "es-ES"),
        VoiceBean("es-ES-LaiaNeural", "西班牙语(西班牙)", "女", SourceType.MICROSOFT, "es-ES"),
        VoiceBean("es-ES-LiaNeural", "西班牙语(西班牙)", "女", SourceType.MICROSOFT, "es-ES"),
        VoiceBean("es-ES-NilNeural", "西班牙语(西班牙)", "男", SourceType.MICROSOFT, "es-ES"),
        VoiceBean("es-ES-SaulNeural", "西班牙语(西班牙)", "男", SourceType.MICROSOFT, "es-ES"),
        VoiceBean("es-ES-TeoNeural", "西班牙语(西班牙)", "男", SourceType.MICROSOFT, "es-ES"),
        VoiceBean("es-ES-TrianaNeural", "西班牙语(西班牙)", "女", SourceType.MICROSOFT, "es-ES"),
        VoiceBean("es-ES-VeraNeural", "西班牙语(西班牙)", "女", SourceType.MICROSOFT, "es-ES"),
        VoiceBean("es-ES-XimenaNeural", "西班牙语(西班牙)", "女", SourceType.MICROSOFT, "es-ES"),
        VoiceBean("es-ES-Tristan:DragonHDLatestNeural", "西班牙语(西班牙)", "男", SourceType.MICROSOFT, "es-ES"),
        VoiceBean("es-ES-Ximena:DragonHDLatestNeural", "西班牙语(西班牙)", "女", SourceType.MICROSOFT, "es-ES"),

        // es-GQ: 西班牙语（赤道几内亚）
        VoiceBean("es-GQ-TeresaNeural", "西班牙语（赤道几内亚）", "女", SourceType.MICROSOFT, "es-GQ"),
        VoiceBean("es-GQ-JavierNeural", "西班牙语（赤道几内亚）", "男", SourceType.MICROSOFT, "es-GQ"),

        // es-GT: 西班牙语（危地马拉）
        VoiceBean("es-GT-MartaNeural", "西班牙语（危地马拉）", "女", SourceType.MICROSOFT, "es-GT"),
        VoiceBean("es-GT-AndresNeural", "西班牙语（危地马拉）", "男", SourceType.MICROSOFT, "es-GT"),

        // es-HN: 西班牙语（洪都拉斯）
        VoiceBean("es-HN-KarlaNeural", "西班牙语（洪都拉斯）", "女", SourceType.MICROSOFT, "es-HN"),
        VoiceBean("es-HN-CarlosNeural", "西班牙语（洪都拉斯）", "男", SourceType.MICROSOFT, "es-HN"),

        // es-MX: 西班牙语（墨西哥）
        VoiceBean("es-MX-DaliaNeural", "西班牙语（墨西哥）", "女", SourceType.MICROSOFT, "es-MX"),
        VoiceBean("es-MX-JorgeNeural", "西班牙语（墨西哥）", "男", SourceType.MICROSOFT, "es-MX"),
        VoiceBean("es-MX-DaliaMultilingualNeural", "西班牙语（墨西哥）", "女", SourceType.MICROSOFT, "es-MX"),
        VoiceBean("es-MX-JorgeMultilingualNeural", "西班牙语（墨西哥）", "男", SourceType.MICROSOFT, "es-MX"),
        VoiceBean("es-MX-BeatrizNeural", "西班牙语（墨西哥）", "女", SourceType.MICROSOFT, "es-MX"),
        VoiceBean("es-MX-CandelaNeural", "西班牙语（墨西哥）", "女", SourceType.MICROSOFT, "es-MX"),
        VoiceBean("es-MX-CarlotaNeural", "西班牙语（墨西哥）", "女", SourceType.MICROSOFT, "es-MX"),
        VoiceBean("es-MX-CecilioNeural", "西班牙语（墨西哥）", "男", SourceType.MICROSOFT, "es-MX"),
        VoiceBean("es-MX-GerardoNeural", "西班牙语（墨西哥）", "男", SourceType.MICROSOFT, "es-MX"),
        VoiceBean("es-MX-LarissaNeural", "西班牙语（墨西哥）", "女", SourceType.MICROSOFT, "es-MX"),
        VoiceBean("es-MX-LibertoNeural", "西班牙语（墨西哥）", "男", SourceType.MICROSOFT, "es-MX"),
        VoiceBean("es-MX-LucianoNeural", "西班牙语（墨西哥）", "男", SourceType.MICROSOFT, "es-MX"),
        VoiceBean("es-MX-MarinaNeural", "西班牙语（墨西哥）", "中性", SourceType.MICROSOFT, "es-MX"),
        VoiceBean("es-MX-NuriaNeural", "西班牙语（墨西哥）", "女", SourceType.MICROSOFT, "es-MX"),
        VoiceBean("es-MX-PelayoNeural", "西班牙语（墨西哥）", "男", SourceType.MICROSOFT, "es-MX"),
        VoiceBean("es-MX-RenataNeural", "西班牙语（墨西哥）", "女", SourceType.MICROSOFT, "es-MX"),
        VoiceBean("es-MX-YagoNeural", "西班牙语（墨西哥）", "男", SourceType.MICROSOFT, "es-MX"),

        // es-NI: 西班牙（尼加拉瓜）
        VoiceBean("es-NI-YolandaNeural", "西班牙（尼加拉瓜）", "女", SourceType.MICROSOFT, "es-NI"),
        VoiceBean("es-NI-FedericoNeural", "西班牙（尼加拉瓜）", "男", SourceType.MICROSOFT, "es-NI"),

        // es-PA: 西班牙语（巴拿马）
        VoiceBean("es-PA-MargaritaNeural", "西班牙语（巴拿马）", "女", SourceType.MICROSOFT, "es-PA"),
        VoiceBean("es-PA-RobertoNeural", "西班牙语（巴拿马）", "男", SourceType.MICROSOFT, "es-PA"),

        // es-PE: 西班牙语（秘鲁）
        VoiceBean("es-PE-CamilaNeural", "西班牙语（秘鲁）", "女", SourceType.MICROSOFT, "es-PE"),
        VoiceBean("es-PE-AlexNeural", "西班牙语（秘鲁）", "男", SourceType.MICROSOFT, "es-PE"),

        // es-PR: 西班牙语（波多黎各）
        VoiceBean("es-PR-KarinaNeural", "西班牙语（波多黎各）", "女", SourceType.MICROSOFT, "es-PR"),
        VoiceBean("es-PR-VictorNeural", "西班牙语（波多黎各）", "男", SourceType.MICROSOFT, "es-PR"),

        // es-PY: 西班牙语（巴拉圭）
        VoiceBean("es-PY-TaniaNeural", "西班牙语（巴拉圭）", "女", SourceType.MICROSOFT, "es-PY"),
        VoiceBean("es-PY-MarioNeural", "西班牙语（巴拉圭）", "男", SourceType.MICROSOFT, "es-PY"),

        // es-SV: 西班牙语（萨尔瓦多）
        VoiceBean("es-SV-LorenaNeural", "西班牙语（萨尔瓦多）", "女", SourceType.MICROSOFT, "es-SV"),
        VoiceBean("es-SV-RodrigoNeural", "西班牙语（萨尔瓦多）", "男", SourceType.MICROSOFT, "es-SV"),

        // es-US: 西班牙语（美国）
        VoiceBean("es-US-PalomaNeural", "西班牙语（美国）", "女", SourceType.MICROSOFT, "es-US"),
        VoiceBean("es-US-AlonsoNeural", "西班牙语（美国）", "男", SourceType.MICROSOFT, "es-US"),

        // es-UY: 西班牙语（乌拉圭）
        VoiceBean("es-UY-ValentinaNeural", "西班牙语（乌拉圭）", "女", SourceType.MICROSOFT, "es-UY"),
        VoiceBean("es-UY-MateoNeural", "西班牙语（乌拉圭）", "男", SourceType.MICROSOFT, "es-UY"),

        // es-VE: 西班牙语（委内瑞拉）
        VoiceBean("es-VE-PaolaNeural", "西班牙语（委内瑞拉）", "女", SourceType.MICROSOFT, "es-VE"),
        VoiceBean("es-VE-SebastianNeural", "西班牙语（委内瑞拉）", "男", SourceType.MICROSOFT, "es-VE"),

        // et-EE: 爱沙尼亚语（爱沙尼亚）
        VoiceBean("et-EE-AnuNeural", "爱沙尼亚语（爱沙尼亚）", "女", SourceType.MICROSOFT, "et-EE"),
        VoiceBean("et-EE-KertNeural", "爱沙尼亚语（爱沙尼亚）", "男", SourceType.MICROSOFT, "et-EE"),

        // eu-ES: 巴斯克语
        VoiceBean("eu-ES-AinhoaNeural", "巴斯克语", "女", SourceType.MICROSOFT, "eu-ES"),
        VoiceBean("eu-ES-AnderNeural", "巴斯克语", "男", SourceType.MICROSOFT, "eu-ES"),

        // fa-IR: 波斯语（伊朗）
        VoiceBean("fa-IR-DilaraNeural", "波斯语（伊朗）", "女", SourceType.MICROSOFT, "fa-IR"),
        VoiceBean("fa-IR-FaridNeural", "波斯语（伊朗）", "男", SourceType.MICROSOFT, "fa-IR"),

        // fi-FI: 芬兰语（芬兰）
        VoiceBean("fi-FI-SelmaNeural", "芬兰语（芬兰）", "女", SourceType.MICROSOFT, "fi-FI"),
        VoiceBean("fi-FI-HarriNeural", "芬兰语（芬兰）", "男", SourceType.MICROSOFT, "fi-FI"),
        VoiceBean("fi-FI-NooraNeural", "芬兰语（芬兰）", "女", SourceType.MICROSOFT, "fi-FI"),

        // fil-PH: 菲律宾语（菲律宾）
        VoiceBean("fil-PH-BlessicaNeural", "菲律宾语（菲律宾）", "女", SourceType.MICROSOFT, "fil-PH"),
        VoiceBean("fil-PH-AngeloNeural", "菲律宾语（菲律宾）", "男", SourceType.MICROSOFT, "fil-PH"),

        // fr-BE: 法语（比利时）
        VoiceBean("fr-BE-CharlineNeural", "法语（比利时）", "女", SourceType.MICROSOFT, "fr-BE"),
        VoiceBean("fr-BE-GerardNeural", "法语（比利时）", "男", SourceType.MICROSOFT, "fr-BE"),

        // fr-CA: 法语（加拿大）
        VoiceBean("fr-CA-SylvieNeural", "法语（加拿大）", "女", SourceType.MICROSOFT, "fr-CA"),
        VoiceBean("fr-CA-JeanNeural", "法语（加拿大）", "男", SourceType.MICROSOFT, "fr-CA"),
        VoiceBean("fr-CA-AntoineNeural", "法语（加拿大）", "男", SourceType.MICROSOFT, "fr-CA"),
        VoiceBean("fr-CA-ThierryNeural", "法语（加拿大）", "男", SourceType.MICROSOFT, "fr-CA"),

        // fr-CH: 法语（瑞士）
        VoiceBean("fr-CH-ArianeNeural", "法语（瑞士）", "女", SourceType.MICROSOFT, "fr-CH"),
        VoiceBean("fr-CH-FabriceNeural", "法语（瑞士）", "男", SourceType.MICROSOFT, "fr-CH"),

        // fr-FR: 法语（法国）
        VoiceBean("fr-FR-DeniseNeural", "法语（法国）", "女", SourceType.MICROSOFT, "fr-FR"),
        VoiceBean("fr-FR-HenriNeural", "法语（法国）", "男", SourceType.MICROSOFT, "fr-FR"),
        VoiceBean("fr-FR-VivienneMultilingualNeural", "法语（法国）", "女", SourceType.MICROSOFT, "fr-FR"),
        VoiceBean("fr-FR-RemyMultilingualNeural", "法语（法国）", "男", SourceType.MICROSOFT, "fr-FR"),
        VoiceBean("fr-FR-LucienMultilingualNeural", "法语（法国）", "男", SourceType.MICROSOFT, "fr-FR"),
        VoiceBean("fr-FR-AlainNeural", "法语（法国）", "男", SourceType.MICROSOFT, "fr-FR"),
        VoiceBean("fr-FR-BrigitteNeural", "法语（法国）", "女", SourceType.MICROSOFT, "fr-FR"),
        VoiceBean("fr-FR-CelesteNeural", "法语（法国）", "女", SourceType.MICROSOFT, "fr-FR"),
        VoiceBean("fr-FR-ClaudeNeural", "法语（法国）", "男", SourceType.MICROSOFT, "fr-FR"),
        VoiceBean("fr-FR-CoralieNeural", "法语（法国）", "女", SourceType.MICROSOFT, "fr-FR"),
        VoiceBean("fr-FR-EloiseNeural", "法语（法国）", "中性", SourceType.MICROSOFT, "fr-FR"),
        VoiceBean("fr-FR-JacquelineNeural", "法语（法国）", "女", SourceType.MICROSOFT, "fr-FR"),
        VoiceBean("fr-FR-JeromeNeural", "法语（法国）", "男", SourceType.MICROSOFT, "fr-FR"),
        VoiceBean("fr-FR-JosephineNeural", "法语（法国）", "女", SourceType.MICROSOFT, "fr-FR"),
        VoiceBean("fr-FR-MauriceNeural", "法语（法国）", "男", SourceType.MICROSOFT, "fr-FR"),
        VoiceBean("fr-FR-YvesNeural", "法语（法国）", "男", SourceType.MICROSOFT, "fr-FR"),
        VoiceBean("fr-FR-YvetteNeural", "法语（法国）", "女", SourceType.MICROSOFT, "fr-FR"),
        VoiceBean("fr-FR-Remy:DragonHDLatestNeural", "法语（法国）", "男", SourceType.MICROSOFT, "fr-FR"),
        VoiceBean("fr-FR-Vivienne:DragonHDLatestNeural", "法语（法国）", "女", SourceType.MICROSOFT, "fr-FR"),

        // ga-IE: 爱尔兰语（爱尔兰）
        VoiceBean("ga-IE-OrlaNeural", "爱尔兰语（爱尔兰）", "女", SourceType.MICROSOFT, "ga-IE"),
        VoiceBean("ga-IE-ColmNeural", "爱尔兰语（爱尔兰）", "男", SourceType.MICROSOFT, "ga-IE"),

        // gl-ES: 加利西亚语
        VoiceBean("gl-ES-SabelaNeural", "加利西亚语", "女", SourceType.MICROSOFT, "gl-ES"),
        VoiceBean("gl-ES-RoiNeural", "加利西亚语", "男", SourceType.MICROSOFT, "gl-ES"),

        // gu-IN: 古吉拉特语（印度）
        VoiceBean("gu-IN-DhwaniNeural", "古吉拉特语（印度）", "女", SourceType.MICROSOFT, "gu-IN"),
        VoiceBean("gu-IN-NiranjanNeural", "古吉拉特语（印度）", "男", SourceType.MICROSOFT, "gu-IN"),

        // he-IL: 希伯来语（以色列）
        VoiceBean("he-IL-HilaNeural", "希伯来语（以色列）", "女", SourceType.MICROSOFT, "he-IL"),
        VoiceBean("he-IL-AvriNeural", "希伯来语（以色列）", "男", SourceType.MICROSOFT, "he-IL"),

        // hi-IN: 印地语（印度）
        VoiceBean("hi-IN-AaravNeural", "印地语（印度）", "男", SourceType.MICROSOFT, "hi-IN"),
        VoiceBean("hi-IN-AnanyaNeural", "印地语（印度）", "女", SourceType.MICROSOFT, "hi-IN"),
        VoiceBean("hi-IN-AartiNeural", "印地语（印度）", "女", SourceType.MICROSOFT, "hi-IN"),
        VoiceBean("hi-IN-ArjunNeural", "印地语（印度）", "男", SourceType.MICROSOFT, "hi-IN"),
        VoiceBean("hi-IN-KavyaNeural", "印地语（印度）", "女", SourceType.MICROSOFT, "hi-IN"),
        VoiceBean("hi-IN-KunalNeural", "印地语（印度）", "男", SourceType.MICROSOFT, "hi-IN"),
        VoiceBean("hi-IN-RehaanNeural", "印地语（印度）", "男", SourceType.MICROSOFT, "hi-IN"),
        VoiceBean("hi-IN-SwaraNeural", "印地语（印度）", "女", SourceType.MICROSOFT, "hi-IN"),
        VoiceBean("hi-IN-MadhurNeural", "印地语（印度）", "男", SourceType.MICROSOFT, "hi-IN"),

        // hr-HR: 克罗地亚语（克罗地亚）
        VoiceBean("hr-HR-GabrijelaNeural", "克罗地亚语（克罗地亚）", "女", SourceType.MICROSOFT, "hr-HR"),
        VoiceBean("hr-HR-SreckoNeural", "克罗地亚语（克罗地亚）", "男", SourceType.MICROSOFT, "hr-HR"),

        // hu-HU: 匈牙利语（匈牙利）
        VoiceBean("hu-HU-NoemiNeural", "匈牙利语（匈牙利）", "女", SourceType.MICROSOFT, "hu-HU"),
        VoiceBean("hu-HU-TamasNeural", "匈牙利语（匈牙利）", "男", SourceType.MICROSOFT, "hu-HU"),

        // hy-AM: 亚美尼亚语（亚美尼亚）
        VoiceBean("hy-AM-AnahitNeural", "亚美尼亚语（亚美尼亚）", "女", SourceType.MICROSOFT, "hy-AM"),
        VoiceBean("hy-AM-HaykNeural", "亚美尼亚语（亚美尼亚）", "男", SourceType.MICROSOFT, "hy-AM"),

        // id-ID: 印度尼西亚语（印度尼西亚）
        VoiceBean("id-ID-GadisNeural", "印度尼西亚语（印度尼西亚）", "女", SourceType.MICROSOFT, "id-ID"),
        VoiceBean("id-ID-ArdiNeural", "印度尼西亚语（印度尼西亚）", "男", SourceType.MICROSOFT, "id-ID"),

        // is-IS: 冰岛语（冰岛）
        VoiceBean("is-IS-GudrunNeural", "冰岛语（冰岛）", "女", SourceType.MICROSOFT, "is-IS"),
        VoiceBean("is-IS-GunnarNeural", "冰岛语（冰岛）", "男", SourceType.MICROSOFT, "is-IS"),

        // it-IT: 意大利语（意大利）
        VoiceBean("it-IT-ElsaNeural", "意大利语（意大利）", "女", SourceType.MICROSOFT, "it-IT"),
        VoiceBean("it-IT-IsabellaNeural", "意大利语（意大利）", "女", SourceType.MICROSOFT, "it-IT"),
        VoiceBean("it-IT-DiegoNeural", "意大利语（意大利）", "男", SourceType.MICROSOFT, "it-IT"),
        VoiceBean("it-IT-AlessioMultilingualNeural", "意大利语（意大利）", "男", SourceType.MICROSOFT, "it-IT"),
        VoiceBean("it-IT-IsabellaMultilingualNeural", "意大利语（意大利）", "女", SourceType.MICROSOFT, "it-IT"),
        VoiceBean("it-IT-GiuseppeMultilingualNeural", "意大利语（意大利）", "男", SourceType.MICROSOFT, "it-IT"),
        VoiceBean("it-IT-MarcelloMultilingualNeural", "意大利语（意大利）", "男", SourceType.MICROSOFT, "it-IT"),
        VoiceBean("it-IT-BenignoNeural", "意大利语（意大利）", "男", SourceType.MICROSOFT, "it-IT"),
        VoiceBean("it-IT-CalimeroNeural", "意大利语（意大利）", "男", SourceType.MICROSOFT, "it-IT"),
        VoiceBean("it-IT-CataldoNeural", "意大利语（意大利）", "男", SourceType.MICROSOFT, "it-IT"),
        VoiceBean("it-IT-FabiolaNeural", "意大利语（意大利）", "女", SourceType.MICROSOFT, "it-IT"),
        VoiceBean("it-IT-FiammaNeural", "意大利语（意大利）", "女", SourceType.MICROSOFT, "it-IT"),
        VoiceBean("it-IT-GianniNeural", "意大利语（意大利）", "男", SourceType.MICROSOFT, "it-IT"),
        VoiceBean("it-IT-GiuseppeNeural", "意大利语（意大利）", "男", SourceType.MICROSOFT, "it-IT"),
        VoiceBean("it-IT-ImeldaNeural", "意大利语（意大利）", "女", SourceType.MICROSOFT, "it-IT"),
        VoiceBean("it-IT-IrmaNeural", "意大利语（意大利）", "女", SourceType.MICROSOFT, "it-IT"),
        VoiceBean("it-IT-LisandroNeural", "意大利语（意大利）", "男", SourceType.MICROSOFT, "it-IT"),
        VoiceBean("it-IT-PalmiraNeural", "意大利语（意大利）", "女", SourceType.MICROSOFT, "it-IT"),
        VoiceBean("it-IT-PierinaNeural", "意大利语（意大利）", "中性", SourceType.MICROSOFT, "it-IT"),
        VoiceBean("it-IT-RinaldoNeural", "意大利语（意大利）", "男", SourceType.MICROSOFT, "it-IT"),
        VoiceBean("it-IT-Alessio:DragonHDLatestNeural", "意大利语（意大利）", "男", SourceType.MICROSOFT, "it-IT"),
        VoiceBean("it-IT-Isabella:DragonHDLatestNeural", "意大利语（意大利）", "女", SourceType.MICROSOFT, "it-IT"),

        // iu-CANS-CA: 因纽特语（音节文字，加拿大）
        VoiceBean("iu-Cans-CA-SiqiniqNeural", "因纽特语（音节文字，加拿大）", "女", SourceType.MICROSOFT, "iu-CANS-CA"),
        VoiceBean("iu-Cans-CA-TaqqiqNeural", "因纽特语（音节文字，加拿大）", "男", SourceType.MICROSOFT, "iu-CANS-CA"),

        // iu-LATN-CA: 因纽特语（拉丁语，加拿大）
        VoiceBean("iu-Latn-CA-SiqiniqNeural", "因纽特语（拉丁语，加拿大）", "女", SourceType.MICROSOFT, "iu-LATN-CA"),
        VoiceBean("iu-Latn-CA-TaqqiqNeural", "因纽特语（拉丁语，加拿大）", "男", SourceType.MICROSOFT, "iu-LATN-CA"),

        // ja-JP: 日语（日本）
        VoiceBean("ja-JP-NanamiNeural", "日语（日本）", "女", SourceType.MICROSOFT, "ja-JP"),
        VoiceBean("ja-JP-KeitaNeural", "日语（日本）", "男", SourceType.MICROSOFT, "ja-JP"),
        VoiceBean("ja-JP-AoiNeural", "日语（日本）", "女", SourceType.MICROSOFT, "ja-JP"),
        VoiceBean("ja-JP-DaichiNeural", "日语（日本）", "男", SourceType.MICROSOFT, "ja-JP"),
        VoiceBean("ja-JP-MayuNeural", "日语（日本）", "女", SourceType.MICROSOFT, "ja-JP"),
        VoiceBean("ja-JP-NaokiNeural", "日语（日本）", "男", SourceType.MICROSOFT, "ja-JP"),
        VoiceBean("ja-JP-ShioriNeural", "日语（日本）", "女", SourceType.MICROSOFT, "ja-JP"),
        VoiceBean("ja-JP-MasaruMultilingualNeural", "日语（日本）", "男", SourceType.MICROSOFT, "ja-JP"),
        VoiceBean("ja-JP-Masaru:DragonHDLatestNeural", "日语（日本）", "男", SourceType.MICROSOFT, "ja-JP"),
        VoiceBean("ja-JP-Nanami:DragonHDLatestNeural", "日语（日本）", "女", SourceType.MICROSOFT, "ja-JP"),

        // jv-ID: 爪哇语(拉丁语、印度尼西亚)
        VoiceBean("jv-ID-SitiNeural", "爪哇语(拉丁语、印度尼西亚)", "女", SourceType.MICROSOFT, "jv-ID"),
        VoiceBean("jv-ID-DimasNeural", "爪哇语(拉丁语、印度尼西亚)", "男", SourceType.MICROSOFT, "jv-ID"),

        // ka-GE: 格鲁吉亚语（格鲁吉亚）
        VoiceBean("ka-GE-EkaNeural", "格鲁吉亚语（格鲁吉亚）", "女", SourceType.MICROSOFT, "ka-GE"),
        VoiceBean("ka-GE-GiorgiNeural", "格鲁吉亚语（格鲁吉亚）", "男", SourceType.MICROSOFT, "ka-GE"),

        // kk-KZ: 哈萨克语（哈萨克斯坦）
        VoiceBean("kk-KZ-AigulNeural", "哈萨克语（哈萨克斯坦）", "女", SourceType.MICROSOFT, "kk-KZ"),
        VoiceBean("kk-KZ-DauletNeural", "哈萨克语（哈萨克斯坦）", "男", SourceType.MICROSOFT, "kk-KZ"),

        // km-KH: 高棉语（柬埔寨）
        VoiceBean("km-KH-SreymomNeural", "高棉语（柬埔寨）", "女", SourceType.MICROSOFT, "km-KH"),
        VoiceBean("km-KH-PisethNeural", "高棉语（柬埔寨）", "男", SourceType.MICROSOFT, "km-KH"),

        // kn-IN: 卡纳达语（印度）
        VoiceBean("kn-IN-SapnaNeural", "卡纳达语（印度）", "女", SourceType.MICROSOFT, "kn-IN"),
        VoiceBean("kn-IN-GaganNeural", "卡纳达语（印度）", "男", SourceType.MICROSOFT, "kn-IN"),

        // ko-KR: 韩语(韩国)
        VoiceBean("ko-KR-SunHiNeural", "韩语(韩国)", "女", SourceType.MICROSOFT, "ko-KR"),
        VoiceBean("ko-KR-InJoonNeural", "韩语(韩国)", "男", SourceType.MICROSOFT, "ko-KR"),
        VoiceBean("ko-KR-HyunsuMultilingualNeural", "韩语(韩国)", "男", SourceType.MICROSOFT, "ko-KR"),
        VoiceBean("ko-KR-BongJinNeural", "韩语(韩国)", "男", SourceType.MICROSOFT, "ko-KR"),
        VoiceBean("ko-KR-GookMinNeural", "韩语(韩国)", "男", SourceType.MICROSOFT, "ko-KR"),
        VoiceBean("ko-KR-HyunsuNeural", "韩语(韩国)", "男", SourceType.MICROSOFT, "ko-KR"),
        VoiceBean("ko-KR-JiMinNeural", "韩语(韩国)", "女", SourceType.MICROSOFT, "ko-KR"),
        VoiceBean("ko-KR-SeoHyeonNeural", "韩语(韩国)", "女", SourceType.MICROSOFT, "ko-KR"),
        VoiceBean("ko-KR-SoonBokNeural", "韩语(韩国)", "女", SourceType.MICROSOFT, "ko-KR"),
        VoiceBean("ko-KR-YuJinNeural", "韩语(韩国)", "女", SourceType.MICROSOFT, "ko-KR"),

        // lo-LA: 老挝语（老挝）
        VoiceBean("lo-LA-KeomanyNeural", "老挝语（老挝）", "女", SourceType.MICROSOFT, "lo-LA"),
        VoiceBean("lo-LA-ChanthavongNeural", "老挝语（老挝）", "男", SourceType.MICROSOFT, "lo-LA"),

        // lt-LT: 立陶宛语（立陶宛）
        VoiceBean("lt-LT-OnaNeural", "立陶宛语（立陶宛）", "女", SourceType.MICROSOFT, "lt-LT"),
        VoiceBean("lt-LT-LeonasNeural", "立陶宛语（立陶宛）", "男", SourceType.MICROSOFT, "lt-LT"),

        // lv-LV: 拉脱维亚语（拉脱维亚）
        VoiceBean("lv-LV-EveritaNeural", "拉脱维亚语（拉脱维亚）", "女", SourceType.MICROSOFT, "lv-LV"),
        VoiceBean("lv-LV-NilsNeural", "拉脱维亚语（拉脱维亚）", "男", SourceType.MICROSOFT, "lv-LV"),

        // mk-MK: 马其顿语（北马其顿）
        VoiceBean("mk-MK-MarijaNeural", "马其顿语（北马其顿）", "女", SourceType.MICROSOFT, "mk-MK"),
        VoiceBean("mk-MK-AleksandarNeural", "马其顿语（北马其顿）", "男", SourceType.MICROSOFT, "mk-MK"),

        // ml-IN: 马拉雅拉姆语（印度）
        VoiceBean("ml-IN-SobhanaNeural", "马拉雅拉姆语（印度）", "女", SourceType.MICROSOFT, "ml-IN"),
        VoiceBean("ml-IN-MidhunNeural", "马拉雅拉姆语（印度）", "男", SourceType.MICROSOFT, "ml-IN"),

        // mn-MN: 蒙古语（蒙古）
        VoiceBean("mn-MN-YesuiNeural", "蒙古语（蒙古）", "女", SourceType.MICROSOFT, "mn-MN"),
        VoiceBean("mn-MN-BataaNeural", "蒙古语（蒙古）", "男", SourceType.MICROSOFT, "mn-MN"),

        // mr-IN: 马拉地语（印度）
        VoiceBean("mr-IN-AarohiNeural", "马拉地语（印度）", "女", SourceType.MICROSOFT, "mr-IN"),
        VoiceBean("mr-IN-ManoharNeural", "马拉地语（印度）", "男", SourceType.MICROSOFT, "mr-IN"),

        // ms-MY: 马来语（马来西亚）
        VoiceBean("ms-MY-YasminNeural", "马来语（马来西亚）", "女", SourceType.MICROSOFT, "ms-MY"),
        VoiceBean("ms-MY-OsmanNeural", "马来语（马来西亚）", "男", SourceType.MICROSOFT, "ms-MY"),

        // mt-MT: 马耳他语（马耳他）
        VoiceBean("mt-MT-GraceNeural", "马耳他语（马耳他）", "女", SourceType.MICROSOFT, "mt-MT"),
        VoiceBean("mt-MT-JosephNeural", "马耳他语（马耳他）", "男", SourceType.MICROSOFT, "mt-MT"),

        // my-MM: 缅甸语(缅甸)
        VoiceBean("my-MM-NilarNeural", "缅甸语(缅甸)", "女", SourceType.MICROSOFT, "my-MM"),
        VoiceBean("my-MM-ThihaNeural", "缅甸语(缅甸)", "男", SourceType.MICROSOFT, "my-MM"),

        // nb-NO: 书面挪威语（挪威）
        VoiceBean("nb-NO-PernilleNeural", "书面挪威语（挪威）", "女", SourceType.MICROSOFT, "nb-NO"),
        VoiceBean("nb-NO-FinnNeural", "书面挪威语（挪威）", "男", SourceType.MICROSOFT, "nb-NO"),
        VoiceBean("nb-NO-IselinNeural", "书面挪威语（挪威）", "女", SourceType.MICROSOFT, "nb-NO"),

        // ne-NP: 尼泊尔语（尼泊尔）
        VoiceBean("ne-NP-HemkalaNeural", "尼泊尔语（尼泊尔）", "女", SourceType.MICROSOFT, "ne-NP"),
        VoiceBean("ne-NP-SagarNeural", "尼泊尔语（尼泊尔）", "男", SourceType.MICROSOFT, "ne-NP"),

        // nl-BE: 荷兰语（比利时）
        VoiceBean("nl-BE-DenaNeural", "荷兰语（比利时）", "女", SourceType.MICROSOFT, "nl-BE"),
        VoiceBean("nl-BE-ArnaudNeural", "荷兰语（比利时）", "男", SourceType.MICROSOFT, "nl-BE"),

        // nl-NL: 荷兰语（荷兰）
        VoiceBean("nl-NL-FennaNeural", "荷兰语（荷兰）", "女", SourceType.MICROSOFT, "nl-NL"),
        VoiceBean("nl-NL-MaartenNeural", "荷兰语（荷兰）", "男", SourceType.MICROSOFT, "nl-NL"),
        VoiceBean("nl-NL-ColetteNeural", "荷兰语（荷兰）", "女", SourceType.MICROSOFT, "nl-NL"),

        // or-IN: 奥里亚语（印度）
        VoiceBean("or-IN-SubhasiniNeural", "奥里亚语（印度）", "女", SourceType.MICROSOFT, "or-IN"),
        VoiceBean("or-IN-SukantNeural", "奥里亚语（印度）", "男", SourceType.MICROSOFT, "or-IN"),

        // pa-IN: 旁遮普语（印度）
        VoiceBean("pa-IN-OjasNeural", "旁遮普语（印度）", "男", SourceType.MICROSOFT, "pa-IN"),
        VoiceBean("pa-IN-VaaniNeural", "旁遮普语（印度）", "女", SourceType.MICROSOFT, "pa-IN"),

        // pl-PL: 波兰语（波兰）
        VoiceBean("pl-PL-AgnieszkaNeural", "波兰语（波兰）", "女", SourceType.MICROSOFT, "pl-PL"),
        VoiceBean("pl-PL-MarekNeural", "波兰语（波兰）", "男", SourceType.MICROSOFT, "pl-PL"),
        VoiceBean("pl-PL-ZofiaNeural", "波兰语（波兰）", "女", SourceType.MICROSOFT, "pl-PL"),

        // ps-AF: 普什图语（阿富汗）
        VoiceBean("ps-AF-LatifaNeural", "普什图语（阿富汗）", "女", SourceType.MICROSOFT, "ps-AF"),
        VoiceBean("ps-AF-GulNawazNeural", "普什图语（阿富汗）", "男", SourceType.MICROSOFT, "ps-AF"),

        // pt-BR: 葡萄牙语（巴西）
        VoiceBean("pt-BR-FranciscaNeural", "葡萄牙语（巴西）", "女", SourceType.MICROSOFT, "pt-BR"),
        VoiceBean("pt-BR-AntonioNeural", "葡萄牙语（巴西）", "男", SourceType.MICROSOFT, "pt-BR"),
        VoiceBean("pt-BR-MacerioMultilingualNeural", "葡萄牙语（巴西）", "男", SourceType.MICROSOFT, "pt-BR"),
        VoiceBean("pt-BR-ThalitaMultilingualNeural", "葡萄牙语（巴西）", "女", SourceType.MICROSOFT, "pt-BR"),
        VoiceBean("pt-BR-BrendaNeural", "葡萄牙语（巴西）", "女", SourceType.MICROSOFT, "pt-BR"),
        VoiceBean("pt-BR-DonatoNeural", "葡萄牙语（巴西）", "男", SourceType.MICROSOFT, "pt-BR"),
        VoiceBean("pt-BR-ElzaNeural", "葡萄牙语（巴西）", "女", SourceType.MICROSOFT, "pt-BR"),
        VoiceBean("pt-BR-FabioNeural", "葡萄牙语（巴西）", "男", SourceType.MICROSOFT, "pt-BR"),
        VoiceBean("pt-BR-GiovannaNeural", "葡萄牙语（巴西）", "女", SourceType.MICROSOFT, "pt-BR"),
        VoiceBean("pt-BR-HumbertoNeural", "葡萄牙语（巴西）", "男", SourceType.MICROSOFT, "pt-BR"),
        VoiceBean("pt-BR-JulioNeural", "葡萄牙语（巴西）", "男", SourceType.MICROSOFT, "pt-BR"),
        VoiceBean("pt-BR-LeilaNeural", "葡萄牙语（巴西）", "女", SourceType.MICROSOFT, "pt-BR"),
        VoiceBean("pt-BR-LeticiaNeural", "葡萄牙语（巴西）", "中性", SourceType.MICROSOFT, "pt-BR"),
        VoiceBean("pt-BR-ManuelaNeural", "葡萄牙语（巴西）", "女", SourceType.MICROSOFT, "pt-BR"),
        VoiceBean("pt-BR-NicolauNeural", "葡萄牙语（巴西）", "男", SourceType.MICROSOFT, "pt-BR"),
        VoiceBean("pt-BR-ThalitaNeural", "葡萄牙语（巴西）", "女", SourceType.MICROSOFT, "pt-BR"),
        VoiceBean("pt-BR-ValerioNeural", "葡萄牙语（巴西）", "男", SourceType.MICROSOFT, "pt-BR"),
        VoiceBean("pt-BR-YaraNeural", "葡萄牙语（巴西）", "女", SourceType.MICROSOFT, "pt-BR"),
        VoiceBean("pt-BR-Macerio:DragonHDLatestNeural", "葡萄牙语（巴西）", "男", SourceType.MICROSOFT, "pt-BR"),
        VoiceBean("pt-BR-Thalita:DragonHDLatestNeural", "葡萄牙语（巴西）", "女", SourceType.MICROSOFT, "pt-BR"),

        // pt-PT: 葡萄牙语(葡萄牙)
        VoiceBean("pt-PT-RaquelNeural", "葡萄牙语(葡萄牙)", "女", SourceType.MICROSOFT, "pt-PT"),
        VoiceBean("pt-PT-DuarteNeural", "葡萄牙语(葡萄牙)", "男", SourceType.MICROSOFT, "pt-PT"),
        VoiceBean("pt-PT-FernandaNeural", "葡萄牙语(葡萄牙)", "女", SourceType.MICROSOFT, "pt-PT"),

        // ro-RO: 罗马尼亚语（罗马尼亚）
        VoiceBean("ro-RO-AlinaNeural", "罗马尼亚语（罗马尼亚）", "女", SourceType.MICROSOFT, "ro-RO"),
        VoiceBean("ro-RO-EmilNeural", "罗马尼亚语（罗马尼亚）", "男", SourceType.MICROSOFT, "ro-RO"),

        // ru-RU: 俄语（俄罗斯）
        VoiceBean("ru-RU-SvetlanaNeural", "俄语（俄罗斯）", "女", SourceType.MICROSOFT, "ru-RU"),
        VoiceBean("ru-RU-DmitryNeural", "俄语（俄罗斯）", "男", SourceType.MICROSOFT, "ru-RU"),
        VoiceBean("ru-RU-DariyaNeural", "俄语（俄罗斯）", "女", SourceType.MICROSOFT, "ru-RU"),

        // si-LK: 僧伽罗语（斯里兰卡）
        VoiceBean("si-LK-ThiliniNeural", "僧伽罗语（斯里兰卡）", "女", SourceType.MICROSOFT, "si-LK"),
        VoiceBean("si-LK-SameeraNeural", "僧伽罗语（斯里兰卡）", "男", SourceType.MICROSOFT, "si-LK"),

        // sk-SK: 斯洛伐克语（斯洛伐克）
        VoiceBean("sk-SK-ViktoriaNeural", "斯洛伐克语（斯洛伐克）", "女", SourceType.MICROSOFT, "sk-SK"),
        VoiceBean("sk-SK-LukasNeural", "斯洛伐克语（斯洛伐克）", "男", SourceType.MICROSOFT, "sk-SK"),

        // sl-SI: 斯洛文尼亚语（斯洛文尼亚）
        VoiceBean("sl-SI-PetraNeural", "斯洛文尼亚语（斯洛文尼亚）", "女", SourceType.MICROSOFT, "sl-SI"),
        VoiceBean("sl-SI-RokNeural", "斯洛文尼亚语（斯洛文尼亚）", "男", SourceType.MICROSOFT, "sl-SI"),

        // so-SO: 索马里语（索马里）
        VoiceBean("so-SO-UbaxNeural", "索马里语（索马里）", "女", SourceType.MICROSOFT, "so-SO"),
        VoiceBean("so-SO-MuuseNeural", "索马里语（索马里）", "男", SourceType.MICROSOFT, "so-SO"),

        // sq-AL: 阿尔巴尼亚语（阿尔巴尼亚）
        VoiceBean("sq-AL-AnilaNeural", "阿尔巴尼亚语（阿尔巴尼亚）", "女", SourceType.MICROSOFT, "sq-AL"),
        VoiceBean("sq-AL-IlirNeural", "阿尔巴尼亚语（阿尔巴尼亚）", "男", SourceType.MICROSOFT, "sq-AL"),

        // sr-LATN-RS: 塞尔维亚语（拉丁语，塞尔维亚）
        VoiceBean("sr-Latn-RS-NicholasNeural", "塞尔维亚语（拉丁语，塞尔维亚）", "男", SourceType.MICROSOFT, "sr-LATN-RS"),
        VoiceBean("sr-Latn-RS-SophieNeural", "塞尔维亚语（拉丁语，塞尔维亚）", "女", SourceType.MICROSOFT, "sr-LATN-RS"),

        // sr-RS: 塞尔维亚语（西里尔文，塞尔维亚）
        VoiceBean("sr-RS-SophieNeural", "塞尔维亚语（西里尔文，塞尔维亚）", "女", SourceType.MICROSOFT, "sr-RS"),
        VoiceBean("sr-RS-NicholasNeural", "塞尔维亚语（西里尔文，塞尔维亚）", "男", SourceType.MICROSOFT, "sr-RS"),

        // su-ID: 巽他语（印度尼西亚）
        VoiceBean("su-ID-TutiNeural", "巽他语（印度尼西亚）", "女", SourceType.MICROSOFT, "su-ID"),
        VoiceBean("su-ID-JajangNeural", "巽他语（印度尼西亚）", "男", SourceType.MICROSOFT, "su-ID"),

        // sv-SE: 瑞典语（瑞典）
        VoiceBean("sv-SE-SofieNeural", "瑞典语（瑞典）", "女", SourceType.MICROSOFT, "sv-SE"),
        VoiceBean("sv-SE-MattiasNeural", "瑞典语（瑞典）", "男", SourceType.MICROSOFT, "sv-SE"),
        VoiceBean("sv-SE-HilleviNeural", "瑞典语（瑞典）", "女", SourceType.MICROSOFT, "sv-SE"),

        // sw-KE: 斯瓦希里语（肯尼亚）
        VoiceBean("sw-KE-ZuriNeural", "斯瓦希里语（肯尼亚）", "女", SourceType.MICROSOFT, "sw-KE"),
        VoiceBean("sw-KE-RafikiNeural", "斯瓦希里语（肯尼亚）", "男", SourceType.MICROSOFT, "sw-KE"),

        // sw-TZ: 斯瓦希里语（坦桑尼亚）
        VoiceBean("sw-TZ-RehemaNeural", "斯瓦希里语（坦桑尼亚）", "女", SourceType.MICROSOFT, "sw-TZ"),
        VoiceBean("sw-TZ-DaudiNeural", "斯瓦希里语（坦桑尼亚）", "男", SourceType.MICROSOFT, "sw-TZ"),

        // ta-IN: 泰米尔语（印度）
        VoiceBean("ta-IN-PallaviNeural", "泰米尔语（印度）", "女", SourceType.MICROSOFT, "ta-IN"),
        VoiceBean("ta-IN-ValluvarNeural", "泰米尔语（印度）", "男", SourceType.MICROSOFT, "ta-IN"),

        // ta-LK: 泰米尔语（斯里兰卡）
        VoiceBean("ta-LK-SaranyaNeural", "泰米尔语（斯里兰卡）", "女", SourceType.MICROSOFT, "ta-LK"),
        VoiceBean("ta-LK-KumarNeural", "泰米尔语（斯里兰卡）", "男", SourceType.MICROSOFT, "ta-LK"),

        // ta-MY: 泰米尔语（马来西亚）
        VoiceBean("ta-MY-KaniNeural", "泰米尔语（马来西亚）", "女", SourceType.MICROSOFT, "ta-MY"),
        VoiceBean("ta-MY-SuryaNeural", "泰米尔语（马来西亚）", "男", SourceType.MICROSOFT, "ta-MY"),

        // ta-SG: 泰米尔语（新加坡）
        VoiceBean("ta-SG-VenbaNeural", "泰米尔语（新加坡）", "女", SourceType.MICROSOFT, "ta-SG"),
        VoiceBean("ta-SG-AnbuNeural", "泰米尔语（新加坡）", "男", SourceType.MICROSOFT, "ta-SG"),

        // te-IN: 泰卢固语（印度）
        VoiceBean("te-IN-ShrutiNeural", "泰卢固语（印度）", "女", SourceType.MICROSOFT, "te-IN"),
        VoiceBean("te-IN-MohanNeural", "泰卢固语（印度）", "男", SourceType.MICROSOFT, "te-IN"),

        // th-TH: 泰语（泰国）
        VoiceBean("th-TH-PremwadeeNeural", "泰语（泰国）", "女", SourceType.MICROSOFT, "th-TH"),
        VoiceBean("th-TH-NiwatNeural", "泰语（泰国）", "男", SourceType.MICROSOFT, "th-TH"),
        VoiceBean("th-TH-AcharaNeural", "泰语（泰国）", "女", SourceType.MICROSOFT, "th-TH"),

        // tr-TR: 土耳其语 (Türkiye)
        VoiceBean("tr-TR-EmelNeural", "土耳其语 (Türkiye)", "女", SourceType.MICROSOFT, "tr-TR"),
        VoiceBean("tr-TR-AhmetNeural", "土耳其语 (Türkiye)", "男", SourceType.MICROSOFT, "tr-TR"),

        // uk-UA: 乌克兰语（乌克兰）
        VoiceBean("uk-UA-PolinaNeural", "乌克兰语（乌克兰）", "女", SourceType.MICROSOFT, "uk-UA"),
        VoiceBean("uk-UA-OstapNeural", "乌克兰语（乌克兰）", "男", SourceType.MICROSOFT, "uk-UA"),

        // ur-IN: 乌尔都语（印度）
        VoiceBean("ur-IN-GulNeural", "乌尔都语（印度）", "女", SourceType.MICROSOFT, "ur-IN"),
        VoiceBean("ur-IN-SalmanNeural", "乌尔都语（印度）", "男", SourceType.MICROSOFT, "ur-IN"),

        // ur-PK: 乌尔都语（巴基斯坦）
        VoiceBean("ur-PK-UzmaNeural", "乌尔都语（巴基斯坦）", "女", SourceType.MICROSOFT, "ur-PK"),
        VoiceBean("ur-PK-AsadNeural", "乌尔都语（巴基斯坦）", "男", SourceType.MICROSOFT, "ur-PK"),

        // uz-UZ: 乌兹别克语（拉丁语、乌兹别克斯坦）
        VoiceBean("uz-UZ-MadinaNeural", "乌兹别克语（拉丁语、乌兹别克斯坦）", "女", SourceType.MICROSOFT, "uz-UZ"),
        VoiceBean("uz-UZ-SardorNeural", "乌兹别克语（拉丁语、乌兹别克斯坦）", "男", SourceType.MICROSOFT, "uz-UZ"),

        // vi-VN: 越南语（越南）
        VoiceBean("vi-VN-HoaiMyNeural", "越南语（越南）", "女", SourceType.MICROSOFT, "vi-VN"),
        VoiceBean("vi-VN-NamMinhNeural", "越南语（越南）", "男", SourceType.MICROSOFT, "vi-VN"),

        // wuu-CN: 中文（吴语，简体）
        VoiceBean("wuu-CN-XiaotongNeural", "中文（吴语，简体）", "女", SourceType.MICROSOFT, "wuu-CN"),
        VoiceBean("wuu-CN-YunzheNeural", "中文（吴语，简体）", "男", SourceType.MICROSOFT, "wuu-CN"),

        // yue-CN: 中文（粤语，简体）
        VoiceBean("yue-CN-XiaoMinNeural", "中文（粤语，简体）", "女", SourceType.MICROSOFT, "yue-CN"),
        VoiceBean("yue-CN-YunSongNeural", "中文（粤语，简体）", "男", SourceType.MICROSOFT, "yue-CN"),

        // zh-CN: 中文（普通话，简体）
        VoiceBean("zh-CN-XiaoxiaoNeural", "中文（普通话，简体）", "女", SourceType.MICROSOFT, "zh-CN"),
        VoiceBean("zh-CN-YunxiNeural", "中文（普通话，简体）", "男", SourceType.MICROSOFT, "zh-CN"),
        VoiceBean("zh-CN-YunjianNeural", "中文（普通话，简体）", "男", SourceType.MICROSOFT, "zh-CN"),
        VoiceBean("zh-CN-XiaoyiNeural", "中文（普通话，简体）", "女", SourceType.MICROSOFT, "zh-CN"),
        VoiceBean("zh-CN-YunyangNeural", "中文（普通话，简体）", "男", SourceType.MICROSOFT, "zh-CN"),
        VoiceBean("zh-CN-XiaochenNeural", "中文（普通话，简体）", "女", SourceType.MICROSOFT, "zh-CN"),
        VoiceBean("zh-CN-XiaochenMultilingualNeural", "中文（普通话，简体）", "女", SourceType.MICROSOFT, "zh-CN"),
        VoiceBean("zh-CN-XiaohanNeural", "中文（普通话，简体）", "女", SourceType.MICROSOFT, "zh-CN"),
        VoiceBean("zh-CN-XiaomengNeural", "中文（普通话，简体）", "女", SourceType.MICROSOFT, "zh-CN"),
        VoiceBean("zh-CN-XiaomoNeural", "中文（普通话，简体）", "女", SourceType.MICROSOFT, "zh-CN"),
        VoiceBean("zh-CN-XiaoqiuNeural", "中文（普通话，简体）", "女", SourceType.MICROSOFT, "zh-CN"),
        VoiceBean("zh-CN-XiaorouNeural", "中文（普通话，简体）", "女", SourceType.MICROSOFT, "zh-CN"),
        VoiceBean("zh-CN-XiaoruiNeural", "中文（普通话，简体）", "女", SourceType.MICROSOFT, "zh-CN"),
        VoiceBean("zh-CN-XiaoshuangNeural", "中文（普通话，简体）", "中性", SourceType.MICROSOFT, "zh-CN"),
        VoiceBean("zh-CN-XiaoxiaoDialectsNeural", "中文（普通话，简体）", "女", SourceType.MICROSOFT, "zh-CN"),
        VoiceBean("zh-CN-XiaoxiaoMultilingualNeural", "中文（普通话，简体）", "女", SourceType.MICROSOFT, "zh-CN"),
        VoiceBean("zh-CN-XiaoyanNeural", "中文（普通话，简体）", "女", SourceType.MICROSOFT, "zh-CN"),
        VoiceBean("zh-CN-XiaoyouNeural", "中文（普通话，简体）", "中性", SourceType.MICROSOFT, "zh-CN"),
        VoiceBean("zh-CN-XiaoyuMultilingualNeural", "中文（普通话，简体）", "女", SourceType.MICROSOFT, "zh-CN"),
        VoiceBean("zh-CN-XiaozhenNeural", "中文（普通话，简体）", "女", SourceType.MICROSOFT, "zh-CN"),
        VoiceBean("zh-CN-YunfengNeural", "中文（普通话，简体）", "男", SourceType.MICROSOFT, "zh-CN"),
        VoiceBean("zh-CN-YunhaoNeural", "中文（普通话，简体）", "男", SourceType.MICROSOFT, "zh-CN"),
        VoiceBean("zh-CN-YunjieNeural", "中文（普通话，简体）", "男", SourceType.MICROSOFT, "zh-CN"),
        VoiceBean("zh-CN-YunxiaNeural", "中文（普通话，简体）", "男", SourceType.MICROSOFT, "zh-CN"),
        VoiceBean("zh-CN-YunxiaoMultilingualNeural", "中文（普通话，简体）", "男", SourceType.MICROSOFT, "zh-CN"),
        VoiceBean("zh-CN-YunyeNeural", "中文（普通话，简体）", "男", SourceType.MICROSOFT, "zh-CN"),
        VoiceBean("zh-CN-YunyiMultilingualNeural", "中文（普通话，简体）", "男", SourceType.MICROSOFT, "zh-CN"),
        VoiceBean("zh-CN-YunzeNeural", "中文（普通话，简体）", "男", SourceType.MICROSOFT, "zh-CN"),
        VoiceBean("zh-CN-Xiaochen:DragonHDFlashLatestNeural", "中文（普通话，简体）", "女", SourceType.MICROSOFT, "zh-CN"),
        VoiceBean("zh-CN-Xiaoxiao:DragonHDFlashLatestNeural", "中文（普通话，简体）", "女", SourceType.MICROSOFT, "zh-CN"),
        VoiceBean("zh-CN-Xiaoxiao2:DragonHDFlashLatestNeural", "中文（普通话，简体）", "女", SourceType.MICROSOFT, "zh-CN"),
        VoiceBean("zh-CN-YunfanMultilingualNeural", "中文（普通话，简体）", "男", SourceType.MICROSOFT, "zh-CN"),
        VoiceBean("zh-CN-Yunxiao:DragonHDFlashLatestNeural", "中文（普通话，简体）", "男", SourceType.MICROSOFT, "zh-CN"),
        VoiceBean("zh-CN-Yunye:DragonHDFlashLatestNeural", "中文（普通话，简体）", "男", SourceType.MICROSOFT, "zh-CN"),
        VoiceBean("zh-CN-Yunyi:DragonHDFlashLatestNeural", "中文（普通话，简体）", "男", SourceType.MICROSOFT, "zh-CN"),
        VoiceBean("zh-CN-Xiaochen:DragonHDLatestNeural", "中文（普通话，简体）", "女", SourceType.MICROSOFT, "zh-CN"),
        VoiceBean("zh-CN-Yunfan:DragonHDLatestNeural", "中文（普通话，简体）", "男", SourceType.MICROSOFT, "zh-CN"),

        // zh-CN-GUANGXI: 中文（广西口音普通话，简体）
        VoiceBean("zh-CN-guangxi-YunqiNeural", "中文（广西口音普通话，简体）", "男", SourceType.MICROSOFT, "zh-CN-GUANGXI"),

        // zh-CN-henan: 中文（中原官话河南，简体）
        VoiceBean("zh-CN-henan-YundengNeural", "中文（中原官话河南，简体）", "男", SourceType.MICROSOFT, "zh-CN-henan"),

        // zh-CN-liaoning: 中文（东北官话，简体）
        VoiceBean("zh-CN-liaoning-XiaobeiNeural", "中文（东北官话，简体）", "女", SourceType.MICROSOFT, "zh-CN-liaoning"),
        VoiceBean("zh-CN-liaoning-YunbiaoNeural", "中文（东北官话，简体）", "男", SourceType.MICROSOFT, "zh-CN-liaoning"),

        // zh-CN-shaanxi: 中文（中原官话陕西，简体）
        VoiceBean("zh-CN-shaanxi-XiaoniNeural", "中文（中原官话陕西，简体）", "女", SourceType.MICROSOFT, "zh-CN-shaanxi"),

        // zh-CN-shandong: 中文（冀鲁官话，简体）
        VoiceBean("zh-CN-shandong-YunxiangNeural", "中文（冀鲁官话，简体）", "男", SourceType.MICROSOFT, "zh-CN-shandong"),

        // zh-CN-sichuan: 中文（西南普通话，简体）
        VoiceBean("zh-CN-sichuan-YunxiNeural", "中文（西南普通话，简体）", "男", SourceType.MICROSOFT, "zh-CN-sichuan"),

        // zh-HK: 中文（粤语，繁体）
        VoiceBean("zh-HK-HiuMaanNeural", "中文（粤语，繁体）", "女", SourceType.MICROSOFT, "zh-HK"),
        VoiceBean("zh-HK-WanLungNeural", "中文（粤语，繁体）", "男", SourceType.MICROSOFT, "zh-HK"),
        VoiceBean("zh-HK-HiuGaaiNeural", "中文（粤语，繁体）", "女", SourceType.MICROSOFT, "zh-HK"),

        // zh-TW: 中文（台湾普通话，繁体）
        VoiceBean("zh-TW-HsiaoChenNeural", "中文（台湾普通话，繁体）", "女", SourceType.MICROSOFT, "zh-TW"),
        VoiceBean("zh-TW-YunJheNeural", "中文（台湾普通话，繁体）", "男", SourceType.MICROSOFT, "zh-TW"),
        VoiceBean("zh-TW-HsiaoYuNeural", "中文（台湾普通话，繁体）", "女", SourceType.MICROSOFT, "zh-TW"),

        // zu-ZA: 祖鲁语（南非）
        VoiceBean("zu-ZA-ThandoNeural", "祖鲁语（南非）", "女", SourceType.MICROSOFT, "zu-ZA"),
        VoiceBean("zu-ZA-ThembaNeural", "祖鲁语（南非）", "男", SourceType.MICROSOFT, "zu-ZA")
    )
}