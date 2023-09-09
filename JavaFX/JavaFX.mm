<map version="freeplane 1.11.5">
<!--To view this file, download free mind mapping software Freeplane from https://www.freeplane.org -->
<node TEXT="JavaFX项目" FOLDED="false" ID="ID_696401721" CREATED="1610381621824" MODIFIED="1694097832073" STYLE="oval">
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
<hook NAME="AutomaticEdgeColor" COUNTER="1" RULE="ON_BRANCH_CREATION"/>
<node TEXT="项目结构" POSITION="bottom_or_right" ID="ID_771100770" CREATED="1694097832081" MODIFIED="1694097853183">
<edge COLOR="#ff0000"/>
<node TEXT="启动类" ID="ID_1807477881" CREATED="1694097853632" MODIFIED="1694097860931">
<node TEXT="加载配置" ID="ID_778450010" CREATED="1694098258861" MODIFIED="1694098264335"/>
<node TEXT="初始化页面" ID="ID_1558724892" CREATED="1694098007040" MODIFIED="1694098216195">
<node TEXT="预加载页面" ID="ID_616089054" CREATED="1694099521601" MODIFIED="1694099533294">
<node TEXT="Preloader" ID="ID_1621768014" CREATED="1694099533298" MODIFIED="1694099543302"/>
</node>
<node TEXT="菜单" ID="ID_1554371169" CREATED="1694098493528" MODIFIED="1694098498539">
<node TEXT="Menu" ID="ID_593575125" CREATED="1694098773934" MODIFIED="1694098795932"/>
<node TEXT="MenuItem" ID="ID_376923183" CREATED="1694098789366" MODIFIED="1694098800955"/>
<node TEXT="MenuBar" ID="ID_1533723339" CREATED="1694098821761" MODIFIED="1694098826167"/>
</node>
<node TEXT="主页面" ID="ID_1025442712" CREATED="1694098221683" MODIFIED="1694098228411">
<node TEXT="FXMLLoader" ID="ID_1623698322" CREATED="1694098228900" MODIFIED="1694098918163"/>
<node TEXT="Parent" ID="ID_1022535334" CREATED="1694098907006" MODIFIED="1694098991411"/>
<node TEXT="Controller" ID="ID_783379133" CREATED="1694098992148" MODIFIED="1694098997490"/>
<node TEXT="Stage" ID="ID_1007968038" CREATED="1694099011513" MODIFIED="1694099015890"/>
</node>
<node TEXT="其他页面" ID="ID_75440428" CREATED="1694098280922" MODIFIED="1694098285572">
<node TEXT="同上" ID="ID_1109608585" CREATED="1694099112712" MODIFIED="1694099117018"/>
</node>
</node>
<node TEXT="其他" ID="ID_579534320" CREATED="1694098190022" MODIFIED="1694099164310">
<node TEXT="系统托盘" ID="ID_912244341" CREATED="1694099168120" MODIFIED="1694099178297"/>
<node TEXT="事件" ID="ID_347885227" CREATED="1694099178972" MODIFIED="1694099190874"/>
</node>
</node>
<node TEXT="页面模块" ID="ID_1226750990" CREATED="1694097873940" MODIFIED="1694097881190">
<node TEXT="fxml文件" ID="ID_1234415371" CREATED="1694097882764" MODIFIED="1694097897174"/>
<node TEXT="css文件" ID="ID_1900539159" CREATED="1694097897380" MODIFIED="1694097902350"/>
<node TEXT="Controller" ID="ID_1507707750" CREATED="1694097903060" MODIFIED="1694097908113"/>
</node>
</node>
</node>
</map>
