<map version="freeplane 1.11.5">
<!--To view this file, download free mind mapping software Freeplane from https://www.freeplane.org -->
<node TEXT="X-MQTT" FOLDED="false" ID="ID_696401721" CREATED="1610381621824" MODIFIED="1694182310644" STYLE="oval">
<font SIZE="18"/>
<hook NAME="MapStyle">
    <properties edgeColorConfiguration="#808080ff,#ff0000ff,#0000ffff,#00ff00ff,#ff00ffff,#00ffffff,#7c0000ff,#00007cff,#007c00ff,#7c007cff,#007c7cff,#7c7c00ff" fit_to_viewport="false" associatedTemplateLocation="template:/standard-1.6.mm"/>

<map_styles>
<stylenode LOCALIZED_TEXT="styles.root_node" STYLE="oval" UNIFORM_SHAPE="true" VGAP_QUANTITY="24 pt">
<font SIZE="24"/>
<stylenode LOCALIZED_TEXT="styles.predefined" POSITION="bottom_or_right" STYLE="bubble">
<stylenode LOCALIZED_TEXT="default" ID="ID_271890427" ICON_SIZE="12 pt" COLOR="#000000" STYLE="fork">
<arrowlink SHAPE="CUBIC_CURVE" COLOR="#000000" WIDTH="2" TRANSPARENCY="200" DASH="" FONT_SIZE="9" FONT_FAMILY="SansSerif" DESTINATION="ID_271890427" STARTARROW="NONE" ENDARROW="DEFAULT"/>
<font NAME="SansSerif" SIZE="10" BOLD="false" ITALIC="false"/>
<richcontent TYPE="DETAILS" CONTENT-TYPE="plain/auto"/>
<richcontent TYPE="NOTE" CONTENT-TYPE="plain/auto"/>
</stylenode>
<stylenode LOCALIZED_TEXT="defaultstyle.details"/>
<stylenode LOCALIZED_TEXT="defaultstyle.attributes">
<font SIZE="9"/>
</stylenode>
<stylenode LOCALIZED_TEXT="defaultstyle.note" COLOR="#000000" BACKGROUND_COLOR="#ffffff" TEXT_ALIGN="LEFT"/>
<stylenode LOCALIZED_TEXT="defaultstyle.floating">
<edge STYLE="hide_edge"/>
<cloud COLOR="#f0f0f0" SHAPE="ROUND_RECT"/>
</stylenode>
<stylenode LOCALIZED_TEXT="defaultstyle.selection" BACKGROUND_COLOR="#afd3f7" BORDER_COLOR_LIKE_EDGE="false" BORDER_COLOR="#afd3f7"/>
</stylenode>
<stylenode LOCALIZED_TEXT="styles.user-defined" POSITION="bottom_or_right" STYLE="bubble">
<stylenode LOCALIZED_TEXT="styles.topic" COLOR="#18898b" STYLE="fork">
<font NAME="Liberation Sans" SIZE="10" BOLD="true"/>
</stylenode>
<stylenode LOCALIZED_TEXT="styles.subtopic" COLOR="#cc3300" STYLE="fork">
<font NAME="Liberation Sans" SIZE="10" BOLD="true"/>
</stylenode>
<stylenode LOCALIZED_TEXT="styles.subsubtopic" COLOR="#669900">
<font NAME="Liberation Sans" SIZE="10" BOLD="true"/>
</stylenode>
<stylenode LOCALIZED_TEXT="styles.important" ID="ID_67550811">
<icon BUILTIN="yes"/>
<arrowlink COLOR="#003399" TRANSPARENCY="255" DESTINATION="ID_67550811"/>
</stylenode>
</stylenode>
<stylenode LOCALIZED_TEXT="styles.AutomaticLayout" POSITION="bottom_or_right" STYLE="bubble">
<stylenode LOCALIZED_TEXT="AutomaticLayout.level.root" COLOR="#000000" STYLE="oval" SHAPE_HORIZONTAL_MARGIN="10 pt" SHAPE_VERTICAL_MARGIN="10 pt">
<font SIZE="18"/>
</stylenode>
<stylenode LOCALIZED_TEXT="AutomaticLayout.level,1" COLOR="#0033ff">
<font SIZE="16"/>
</stylenode>
<stylenode LOCALIZED_TEXT="AutomaticLayout.level,2" COLOR="#00b439">
<font SIZE="14"/>
</stylenode>
<stylenode LOCALIZED_TEXT="AutomaticLayout.level,3" COLOR="#990000">
<font SIZE="12"/>
</stylenode>
<stylenode LOCALIZED_TEXT="AutomaticLayout.level,4" COLOR="#111111">
<font SIZE="10"/>
</stylenode>
<stylenode LOCALIZED_TEXT="AutomaticLayout.level,5"/>
<stylenode LOCALIZED_TEXT="AutomaticLayout.level,6"/>
<stylenode LOCALIZED_TEXT="AutomaticLayout.level,7"/>
<stylenode LOCALIZED_TEXT="AutomaticLayout.level,8"/>
<stylenode LOCALIZED_TEXT="AutomaticLayout.level,9"/>
<stylenode LOCALIZED_TEXT="AutomaticLayout.level,10"/>
<stylenode LOCALIZED_TEXT="AutomaticLayout.level,11"/>
</stylenode>
</stylenode>
</map_styles>
</hook>
<hook NAME="AutomaticEdgeColor" COUNTER="5" RULE="ON_BRANCH_CREATION"/>
<node TEXT="连接" POSITION="bottom_or_right" ID="ID_33115427" CREATED="1694182312314" MODIFIED="1694183241275">
<edge COLOR="#ff0000"/>
<node TEXT="搜索" ID="ID_1919754197" CREATED="1694183241286" MODIFIED="1694183248636"/>
<node TEXT="创建" ID="ID_684360225" CREATED="1694183248810" MODIFIED="1694183261692"/>
<node TEXT="编辑" ID="ID_1513873228" CREATED="1694183262121" MODIFIED="1694183265963">
<node TEXT="连接信息" ID="ID_1730412113" CREATED="1694186128439" MODIFIED="1694186132752"/>
<node TEXT="动作" ID="ID_1433592388" CREATED="1694186132995" MODIFIED="1694186139087">
<node TEXT="发送" POSITION="bottom_or_right" ID="ID_539405052" CREATED="1694185761706" MODIFIED="1694185913510">
<node TEXT="发送按钮 -&gt; 动作 -&gt; mqtt" ID="ID_542343304" CREATED="1694185829925" MODIFIED="1694186011425"/>
</node>
<node TEXT="接收" POSITION="bottom_or_right" ID="ID_475697287" CREATED="1694185821817" MODIFIED="1694185921386">
<node TEXT="mqtt -&gt; 动作 -&gt; 接收面板" ID="ID_137846511" CREATED="1694185979227" MODIFIED="1694186105391"/>
</node>
</node>
</node>
<node TEXT="删除" ID="ID_77920038" CREATED="1694183498095" MODIFIED="1694183503672"/>
<node TEXT="连接" ID="ID_1652723921" CREATED="1694183266201" MODIFIED="1694183307999"/>
<node TEXT="列表" ID="ID_1839751083" CREATED="1694183545435" MODIFIED="1694183548802"/>
</node>
<node TEXT="动作" POSITION="bottom_or_right" ID="ID_1721175953" CREATED="1694183333337" MODIFIED="1694185691301">
<edge COLOR="#0000ff"/>
<node TEXT="搜索" ID="ID_1893807262" CREATED="1694183348412" MODIFIED="1694183352191"/>
<node TEXT="创建" ID="ID_1853886918" CREATED="1694183352360" MODIFIED="1694183355175"/>
<node TEXT="编辑" ID="ID_961509686" CREATED="1694183355912" MODIFIED="1694183358494"/>
<node TEXT="测试" ID="ID_256300050" CREATED="1694183376000" MODIFIED="1694183379727"/>
<node TEXT="列表" ID="ID_562228956" CREATED="1694183553159" MODIFIED="1694183576632"/>
<node TEXT="功能实现：Janino" ID="ID_828497863" CREATED="1694188023174" MODIFIED="1694188034412"/>
</node>
<node TEXT="日志" POSITION="bottom_or_right" ID="ID_1810011416" CREATED="1694183405389" MODIFIED="1694183408002">
<edge COLOR="#00ff00"/>
<node TEXT="实时" ID="ID_426978833" CREATED="1694184081403" MODIFIED="1694184087858"/>
<node TEXT="文件" ID="ID_334127437" CREATED="1694184088936" MODIFIED="1694184101857"/>
<node TEXT="搜索" ID="ID_603986487" CREATED="1694184400406" MODIFIED="1694184403164"/>
</node>
<node TEXT="设置" POSITION="bottom_or_right" ID="ID_1213027254" CREATED="1694183761914" MODIFIED="1694183764709">
<edge COLOR="#ff00ff"/>
<node TEXT="配置文件" ID="ID_1433040134" CREATED="1694183767059" MODIFIED="1694183771128"/>
</node>
<node TEXT="关于" POSITION="bottom_or_right" ID="ID_6074484" CREATED="1694183772597" MODIFIED="1694183775544">
<edge COLOR="#00ffff"/>
</node>
</node>
</map>
