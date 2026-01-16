package com.autodroid.guardiansdk.ui.why

import android.content.Context
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.autodroid.guardiansdk.R
import com.autodroid.guardiansdk.ui.why.model.WhyItem
import com.autodroid.guardiansdk.ui.why.model.WhyItemType

class WhyViewModel(private val context: Context) : ViewModel() {

    private val _whyItems = MutableLiveData<List<WhyItem>>()
    val whyItems: LiveData<List<WhyItem>> = _whyItems

    fun loadWhyItems() {
        val items = mutableListOf<WhyItem>()

        // Header - 强调植入守卫
        items.add(WhyItem(
            id = 1,
            type = WhyItemType.HEADER,
            title = "为什么选择 Guardian SDK",
            content = "植入守卫 · 隐秘报警 · 绝对安全"
        ))

        // Overview - 说明 SDK 是植入到正常应用中
        items.add(WhyItem(
            id = 2,
            type = WhyItemType.OVERVIEW,
            title = "植入到正常应用中",
            content = "Guardian SDK 可以植入到任何正常的应用中。记事本应用植入 SDK 后就变成记事本守卫，计算器应用植入 SDK 后就变成计算器守卫。坏人检查手机时看到的只是普通的记事本或计算器，完全无法发现报警功能的存在。所有安全功能都在后台静默运行。",
            iconRes = R.drawable.guardian_ic_info
        ))

        // Features - 核心功能列表
        items.add(WhyItem(
            id = 3,
            type = WhyItemType.FEATURES,
            title = "隐秘报警触发方式",
            content = """• 长按浮动窗口：手机上显示的透明小点，长按5秒即可触发报警
                    • 长按音量键：无需看屏幕，盲按音量键即可触发
                    • 摇动手机：通过快速摇动手机触发报警
                    • 长时间未用自动报警：手机被收缴或无法操作时，长时间（如3小时）未使用应用自动触发报警，通知监护人可能遇到危险
                    • 报警时自动录音：无需人工干预，自动开始隐秘录音，记录现场证据
                    • 分段录音+邮件发送：每2分钟自动生成一段录音并加密发送到邮箱，即使手机被损坏证据也不会丢失
                    • 短信自动删除：报警短信发送后自动从短信记录中删除
                    • 位置信息加密：GPS位置使用密码本加密，坏人看不懂
                    • 短信开门密语：给自己手机发送特定密语，自动打开隐秘设置界面
                    • 离线可用：无需网络也能正常工作，纯本地化处理""".trimMargin(),
            iconRes = R.drawable.guardian_ic_check
        ))

        // Benefits - 强调核心价值
        items.add(WhyItem(
            id = 4,
            type = WhyItemType.BENEFITS,
            title = "绝对隐秘的安全保障",
            content = """• 应用功能完全正常：植入后记事本还是记事本，功能不受影响
                    • 防手机检查：坏人翻看应用看到的只是普通功能，无法发现报警系统
                    • 隐秘触发报警：无需打开任何界面，盲操作即可触发，坏人看不见
                    • 分段录音备份：每2分钟自动备份到邮箱，手机被砸也不怕证据丢失
                    • 邮件加密传输：邮箱凭据加密存储，录音文件加密发送
                    • 短信记录自动清除：发送的报警短信自动删除，不留下痕迹
                    • 位置信息加密：GPS坐标转成中文密文，坏人看不懂
                    • 开门密语保护：只有知道密语的人才能打开设置界面
                    • 纯本地化：无服务器依赖，所有数据在本地加密存储""".trimMargin(),
            iconRes = R.drawable.guardian_ic_star
        ))

        // Security Story 1 - 真实案例：小娟跟踪事件
        items.add(WhyItem(
            id = 5,
            type = WhyItemType.SECURITY_STORY,
            title = "真实案例：发现跟踪，男友紧急接应",
            content = """美女小娟下班回家时，发现身后有一个可疑男子一直跟踪她。她不动声色地拿出手机，表面上是在用记事本应用记录工作内容，实际上这个记事本已经植入了 Guardian SDK。小娟趁跟踪者不注意，偷偷长按了屏幕角落的透明小圆点（浮动窗口）15秒，紧急报警被触发，她男朋友立即收到一条报警短信："小娟遇到紧急情况，GPS位置：XXX"。男朋友看到报警后马上开车赶到指定位置接小娟，成功防范了意外发生。跟踪者看到小娟上了男朋友的车，只能悻悻离开。""",
            iconRes = R.drawable.guardian_ic_shield
        ))

        // Security Story 1.5 - 真实案例：王老板缅北脱险
        items.add(WhyItem(
            id = 8,
            type = WhyItemType.SECURITY_STORY,
            title = "真实案例：缅北遇险，死命报警获救",
            content = """王老板到缅北做建材生意，某天在一个偏僻地方谈生意时，突然发现情况不对劲，几个彪形大汉向他围过来。王老板立刻意识到危险，死命抓住手机，假装在看记事本里的合同。就在大汉们要夺走手机的前一刻，王老板拼尽全力长按了屏幕角落的透明小圆点（浮动窗口）15秒！就在手机被夺走的同时，紧急报警短信已经发送给了他的贴身保镖："老板遇到极度危险，GPS位置：缅北XXX"。保镖收到报警后立即带领武装力量火速赶往现场，大汉们还没来得及把王老板带走，保镖就冲了进来。王老板成功脱离危险！这15秒的拼命按住浮动窗口救了他一命！""",
            iconRes = R.drawable.guardian_ic_emergency_contact
        ))

        // Security Story 2 - 真实案例：小李遭遇霸凌
        items.add(WhyItem(
            id = 6,
            type = WhyItemType.SECURITY_STORY,
            title = "真实案例：遭遇霸凌，录音存证维权",
            content = """小李在公园遇到几个大汉欺负人，情况危急。他口袋里按住手机音量键，触发了隐秘报警。系统立即开始分段录音，每录完2分钟就自动加密并通过邮件发送到小李的邮箱。整个过程中，不管手机是否被抢走或损坏，已经发送的录音证据都安全地保存在邮箱中。第二天，小李的家人查看邮箱，收到了8段完整的现场录音，总计16分钟的霸凌证据。有了这些铁证，警方很快抓获了霸凌者，受害人也获得了应有的赔偿。分段录音+邮件发送确保了即使手机被砸，证据也不会丢失。""",
            iconRes = R.drawable.guardian_ic_emergency_contact
        ))

        // Security Story 3 - 真实案例：手机被收缴后自动报警
        items.add(WhyItem(
            id = 7,
            type = WhyItemType.SECURITY_STORY,
            title = "真实案例：手机被收缴后自动报警",
            content = """小张被坏人挟持后，手机被收缴了，她完全无法手动操作手机。但因为她的记事本应用植入了 Guardian SDK，系统检测到3小时内没有使用记事本应用，自动触发了报警。监护人立即收到了一条报警短信："手机超过3小时未使用，可能遇到危险，当前位置XXX"。监护人立即报警求助，小张最终成功获救。这个自动报警功能在小张完全无法操作手机的情况下救了她。""",
            iconRes = R.drawable.guardian_ic_check
        ))

        // Security Story 4 - 真实案例：网约车遇险
        items.add(WhyItem(
            id = 10,
            type = WhyItemType.SECURITY_STORY,
            title = "真实案例：网约车偏航，摇动手机救命",
            content = """美女小林深夜独自坐网约车回家，司机突然改变路线开往偏僻地方。小林意识到不对劲，但司机正盯着她，不敢有太大动作。她悄悄把手机握在手里，假装在看微信，实际上是在记事本应用里假装记东西。就在车拐进小路的前一刻，小林快速摇动手机3次，触发了隐秘报警！家人立即收到报警短信："小林遇到紧急情况，GPS位置正在偏航"。定位显示车已经开到荒郊野外，家人立刻报警并联系网约车平台。警察在5分钟内赶到，成功抓获了意图不轨的司机。摇动手机触发报警在司机完全不知道的情况下救了小林！""",
            iconRes = R.drawable.guardian_ic_shield
        ))

        // Security Story 5 - 真实案例：家暴受害者求救
        items.add(WhyItem(
            id = 11,
            type = WhyItemType.SECURITY_STORY,
            title = "真实案例：家暴现场，录音存证报警",
            content = """小芳长期遭受丈夫家暴，每次报警都被她丈夫发现并销毁证据。后来小芳的闺蜜给她推荐了植入了 Guardian SDK 的计算器应用。某天丈夫再次施暴时，小芳口袋里按住手机音量键，触发了隐秘报警。系统立即开始分段录音，每2分钟自动加密并通过邮件发送到小芳的备用邮箱。整个家暴过程持续了30分钟，但每段录音都安全地保存在邮箱里，丈夫完全不知道。第二天，小芳在闺蜜家打开邮箱，收到了15段完整的家暴录音证据，总计30分钟。警方根据这些录音证据立即逮捕了丈夫，小芳终于获得了人身自由和离婚支持！""",
            iconRes = R.drawable.guardian_ic_emergency_contact
        ))

        // Security Story 6 - 真实案例：学生放学路上遇险
        items.add(WhyItem(
            id = 12,
            type = WhyItemType.SECURITY_STORY,
            title = "真实案例：放学路上被绑架，长按音量键救命",
            content = """13岁的小学生小明放学路上，被几个校外不良少年拉到偏僻巷子里威胁要钱。小明手里拿着手机，坏人以为他在给家里打电话要钱，实际上小明已经悄悄按住音量键10秒。植入了 Guardian SDK 的手机立即发送报警短信："小明被威胁要钱，GPS位置XXX"。收到短信后，小明爸爸立刻赶到现场，不良少年们吓得四散逃跑。爸爸保护了小明并立即报警处理。小明的冷静和快速反应，让坏人完全没有察觉他在求救！""",
            iconRes = R.drawable.guardian_ic_check
        ))

        // Security Story 7 - 真实案例：职场性骚扰
        items.add(WhyItem(
            id = 13,
            type = WhyItemType.SECURITY_STORY,
            title = "真实案例：职场性骚扰，录音留证维权",
            content = """小白刚入职一家公司，老板经常在办公室对她进行言语和行为上的性骚扰。小白害怕丢工作不敢直接反抗，但她植入了 Guardian SDK 的笔记应用帮她留了后手。某天老板把她叫到办公室锁门要对她动手动脚，小白假装拿出手机记录工作要点，实际上已经偷偷长按了浮动窗口15秒。隐秘报警被触发，系统开始录音并将录音实时发送到小白的公司邮箱。整个性骚扰过程被完整记录下来。第二天，小白带着录音证据去人事部投诉，老板被立即开除，小白获得了应有的赔偿和道歉。如果没有这些录音，小白根本无法维权！""",
            iconRes = R.drawable.guardian_ic_shield
        ))

        // Security Story 8 - 真实案例：旅游被诈骗团伙挟持
        items.add(WhyItem(
            id = 14,
            type = WhyItemType.SECURITY_STORY,
            title = "真实案例：旅游遇骗，手机被夺前一刻报警",
            content = """退休老人王阿姨参加低价旅游团，被带到了缅北的诈骗团伙窝点。骗团伙没收了所有人的手机，但王阿姨反应最快，在手机被夺走的前一秒死命按住了浮动窗口20秒！就在手机被抢走的同时，一条报警短信已经发给了她的儿子："妈妈被骗到缅北，GPS位置XXX"。儿子收到短信后立即报警并联系大使馆。因为有了准确位置，警方在3天内成功端掉了这个诈骗团伙，解救了包括王阿姨在内的30多名受害者。王阿姨的这20秒拼命按住浮动窗口救了30多个人！""",
            iconRes = R.drawable.guardian_ic_emergency_contact
        ))

        // Security Story 9 - 真实案例：夜跑遭遇袭击
        items.add(WhyItem(
            id = 15,
            type = WhyItemType.SECURITY_STORY,
            title = "真实案例：夜跑遇袭，摇动手机求救",
            content = """健身爱好者小李晚上独自在公园跑步，突然从后面冲出一个歹徒要抢劫。歹徒拿着刀威胁，小李不敢有太大动作，只能一边假装配合一边把手机握在手里。就在歹徒要抢走手机的前一刻，小李快速摇动手机5次，触发了隐秘报警！定位显示小李在公园深处，家人立即收到报警并报警。警察在5分钟内赶到，歹徒还没来得及逃跑就被抓获。摇动手机报警在歹徒完全没有察觉的情况下救了小李！""",
            iconRes = R.drawable.guardian_ic_check
        ))

        // Security Story 10 - 真实案例：网贷暴力催收
        items.add(WhyItem(
            id = 16,
            type = WhyItemType.SECURITY_STORY,
            title = "真实案例：网贷催收上门，录音存证报警",
            content = """小张因为网贷逾期，催收公司派人上门威胁恐吓。催收人员强行闯入他家，大吵大闹，威胁要打砸东西。小张趁他们不注意，悄悄按住手机音量键触发了隐秘报警。系统立即开始分段录音，每2分钟自动加密并通过邮件发送到小张的云盘邮箱。整个催收过程持续了40分钟，催收人员的所有威胁言论都被完整录音。第二天，小张带着这些证据去派出所报案，警方根据证据端掉了这个非法催收团伙，小张获得了法律保护，再也不用担心暴力威胁！""",
            iconRes = R.drawable.guardian_ic_emergency_contact
        ))

        // Security Story 11 - 真实案例：耳聋老人迷路，短信查询定位
        items.add(WhyItem(
            id = 17,
            type = WhyItemType.SECURITY_STORY,
            title = "真实案例：耳聋老人迷路，短信查询定位救人",
            content = """70岁的王大爷双耳失聪，完全听不到电话。某天他独自去买菜，走到陌生区域迷路了，怎么也找不回家。到了晚饭时间，儿子发现爸爸还没回来，打了很多次电话都无人接听。儿子很着急，突然想起爸爸手机里植入了 Guardian SDK 的记事本守卫。儿子立刻给爸爸发了一条查询短信："爸，你在哪里？" 王大爷的手机收到短信后，记事本守卫的 AccessibilityService 自动监控到这条查询，立即回复："我迷路了，GPS位置XXX"。儿子收到回复后马上开车赶到那个位置，发现爸爸正茫然地在街头徘徊。有了 Guardian SDK 的短信查询功能，耳聋老人即使接不到电话，也能通过短信安全定位！""",
            iconRes = R.drawable.guardian_ic_check
        ))

        // Security Story 12 - 真实案例：痴呆老人走失，多方短信查询
        items.add(WhyItem(
            id = 18,
            type = WhyItemType.SECURITY_STORY,
            title = "真实案例：痴呆老人走失，多方短信查询寻回",
            content = """65岁的李奶奶患有轻度阿尔茨海默病，有时会忘记回家的路。某天傍晚她独自去公园散步，结果天黑了还没回来。儿子、女儿、老伴都给她打电话，但李奶奶患有痴呆，经常忘记接电话或乱按。一家人都很着急，突然想到李奶奶手机里植入了 Guardian SDK 的计算器守卫。三个人同时给李奶奶发查询短信："妈/奶奶/老婆，你在哪里？" 记事本守卫自动监控到查询短信，立即回复："我在公园，GPS位置XXX"。一家人看到回复后立刻赶到公园，发现李奶奶正坐在长椅上发呆，完全不记得自己是怎么走过去的。如果没有 Guardian SDK 的自动查询回复功能，全家人可能要找一整夜！""",
            iconRes = R.drawable.guardian_ic_shield
        ))

        // Security Story 13 - 真实案例：独居老人摔倒，音量键求救
        items.add(WhyItem(
            id = 19,
            type = WhyItemType.SECURITY_STORY,
            title = "真实案例：独居老人摔倒，音量键隐秘求救",
            content = """78岁的张爷爷独居，儿女都在外地工作。某天他在卫生间滑倒摔骨折，躺在地上起不来，手机也掉在离他几米远的地方。张爷爷痛得叫不出声，但用脚尖慢慢把手机踢到身边。他捡起手机，发现是植入了 Guardian SDK 的记事本守卫。老人虽然不会用复杂功能，但他知道可以按音量键求救。张爷爷拼尽全力按住音量键10秒，隐秘报警立即触发！远在外地的女儿收到报警短信："爸爸遇到紧急情况，GPS位置XXX"。女儿立刻联系120急救和邻居破门进入，及时发现张爷爷并送医救治。医生说如果再晚半小时，老人可能有生命危险！""",
            iconRes = R.drawable.guardian_ic_emergency_contact
        ))

        // Security Story 14 - 真实案例：老人被骗离家，短信查询定位
        items.add(WhyItem(
            id = 20,
            type = WhyItemType.SECURITY_STORY,
            title = "真实案例：老人被骗离家，短信查询定位寻回",
            content = """赵大爷退休在家，接到陌生电话说中了100万大奖，需要到偏远地方领奖。赵大爷激动之下，告诉老伴要去领奖，老伴觉得可疑但拦不住。赵大爷到了指定地方，发现是诈骗团伙，但对方强行拉他去"领奖中心"。赵大爷虽然年纪大但不糊涂，意识到上当后假装配合。老伴在家很着急，发现大爷几小时没回电话，想起他的手机植入了 Guardian SDK。老伴立即发查询短信："老头子，你在哪里？" 记事本守卫自动回复："我被骗了，GPS位置XXX"。老伴收到回复后立刻报警并提供位置，警方在半小时内赶到，成功端掉了这个针对老年人的诈骗团伙，解救了包括赵大爷在内的10多名老人！""",
            iconRes = R.drawable.guardian_ic_check
        ))

        // Security Story 15 - 真实案例：低血糖老人晕倒，摇动手机求救
        items.add(WhyItem(
            id = 21,
            type = WhyItemType.SECURITY_STORY,
            title = "真实案例：低血糖老人晕倒，摇动手机求救",
            content = """68岁的王奶奶患有糖尿病，经常低血糖晕倒。某天她独自在家，突然低血糖发作晕倒在沙发上，意识模糊。王奶奶虽然清醒，但浑身无力，说不出话。她手里还握着手机，想起儿子说过可以摇动手机求救。王奶奶拼尽全力摇动手机5次，隐秘报警被触发！在外地工作的儿子收到报警短信："妈妈遇到紧急情况，GPS位置XXX"。儿子立刻联系住在附近的表哥上门，表哥破门进入后发现王奶奶已经昏迷，立即喂糖并送医。医生说如果再晚20分钟，可能就救不回来了！""",
            iconRes = R.drawable.guardian_ic_shield
        ))

        // 定制服务广告
        items.add(WhyItem(
            id = 9,
            type = WhyItemType.SECURITY_STORY,
            title = "🛡️ SDK 个性化定制服务",
            content = """为了最大限度地防止坏人识别出手机安装了报警 SDK，我们提供专业的 SDK 定制服务。

• 修改包名：将公开 SDK 包名完全替换为自定义包名，无法通过包名识别
• 修改组件名称：AccessibilityService、Receiver 等组件名称全部自定义，从应用信息中看不出任何痕迹
• 代码混淆：增强代码混淆等级，反编译后难以识别
• 资源重命名：所有资源文件名、字符串资源全部自定义，从 APK 中搜索不到特征
• 签名定制：使用自定义签名文件，避免公开签名被识别

定制后的 SDK 与公开版本完全不同，坏人无法通过任何公开信息检测手机是否安装了报警 SDK！

联系我们获取定制服务，确保您的绝对安全。""",
            iconRes = R.drawable.guardian_ic_shield
        ))

        _whyItems.value = items
    }
}