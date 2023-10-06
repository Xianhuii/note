<map version="freeplane 1.11.5">
<!--To view this file, download free mind mapping software Freeplane from https://www.freeplane.org -->
<node TEXT="编程之路" FOLDED="false" ID="ID_696401721" CREATED="1610381621824" MODIFIED="1694525762863" STYLE="oval">
<font SIZE="18"/>
<hook NAME="MapStyle" zoom="2.301">
    <properties edgeColorConfiguration="#808080ff,#ff0000ff,#0000ffff,#00ff00ff,#ff00ffff,#00ffffff,#7c0000ff,#00007cff,#007c00ff,#7c007cff,#007c7cff,#7c7c00ff" show_icon_for_attributes="true" associatedTemplateLocation="template:/standard-1.6.mm" show_note_icons="true" followedTemplateLocation="template:/standard-1.6.mm" followedMapLastTime="1692705696000" fit_to_viewport="false"/>

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
<hook NAME="AutomaticEdgeColor" COUNTER="15" RULE="ON_BRANCH_CREATION"/>
<node TEXT="编程语言" FOLDED="true" POSITION="top_or_left" ID="ID_852257794" CREATED="1694526591818" MODIFIED="1694526598628">
<edge COLOR="#7c007c"/>
<node TEXT="Java" POSITION="top_or_left" ID="ID_224740183" CREATED="1694525762874" MODIFIED="1694603778504">
<icon BUILTIN="button_ok"/>
</node>
</node>
<node TEXT="框架" FOLDED="true" POSITION="top_or_left" ID="ID_750680347" CREATED="1694526610126" MODIFIED="1694526613884">
<edge COLOR="#007c7c"/>
<node TEXT="Spring" POSITION="top_or_left" ID="ID_177282307" CREATED="1694525879836" MODIFIED="1694603786271">
<icon BUILTIN="button_ok"/>
</node>
<node TEXT="Mybatis" POSITION="top_or_left" ID="ID_1269392219" CREATED="1694525893114" MODIFIED="1694603789967">
<icon BUILTIN="button_ok"/>
</node>
</node>
<node TEXT="数据库" POSITION="top_or_left" ID="ID_1928359821" CREATED="1694526625737" MODIFIED="1694611028639">
<edge COLOR="#7c7c00"/>
<node TEXT="MySQL" POSITION="top_or_left" ID="ID_1849740645" CREATED="1694525888087" MODIFIED="1694526632980">
<node TEXT="索引" FOLDED="true" ID="ID_769539823" CREATED="1694604106766" MODIFIED="1694604109461">
<node TEXT="选择性" ID="ID_1928094727" CREATED="1694611762030" MODIFIED="1694611769631">
<node TEXT="不重复的索引值个数/记录总数" ID="ID_1338452281" CREATED="1694611770377" MODIFIED="1694611798911"/>
</node>
<node TEXT="前缀索引" ID="ID_306632655" CREATED="1694610933957" MODIFIED="1694610943566"/>
<node TEXT="索引合并index_merge（UNION优化）" ID="ID_153362649" CREATED="1694610947871" MODIFIED="1694611511128">
<node TEXT="OR条件联合" ID="ID_798008190" CREATED="1694611109230" MODIFIED="1694611119136"/>
<node TEXT="And条件相交" ID="ID_648239095" CREATED="1694611119309" MODIFIED="1694611129448"/>
</node>
<node TEXT="联合索引" ID="ID_1804516775" CREATED="1694611345216" MODIFIED="1694611547367"/>
<node TEXT="聚簇&amp;非聚簇索引" ID="ID_1399011183" CREATED="1694610956127" MODIFIED="1694611076648"/>
<node TEXT="覆盖索引" ID="ID_1851763454" CREATED="1694610974123" MODIFIED="1694610979572"/>
<node TEXT="排序" ID="ID_557069280" CREATED="1694612502973" MODIFIED="1694612510166">
<node TEXT="字段顺序一致" ID="ID_1279426286" CREATED="1694614379200" MODIFIED="1694614419423"/>
<node TEXT="排序顺序一致" ID="ID_476683440" CREATED="1694614382828" MODIFIED="1694614389450"/>
<node TEXT="前导列为常量" ID="ID_1085955437" CREATED="1694614429404" MODIFIED="1694614438001"/>
<node TEXT="全在第一个表" ID="ID_97199039" CREATED="1694614476923" MODIFIED="1694614485069"/>
</node>
<node TEXT="冗余索引" ID="ID_1351096985" CREATED="1694614807201" MODIFIED="1694614819547"/>
<node TEXT="未使用的索引" ID="ID_208168874" CREATED="1694614999416" MODIFIED="1694615007901">
<node TEXT="SELECT * FROM sys.schema_unused_indexs" ID="ID_328481797" CREATED="1694615008882" MODIFIED="1694615035561"/>
</node>
</node>
<node TEXT="查询性能优化" ID="ID_982072687" CREATED="1694615518060" MODIFIED="1694615525574">
<node TEXT="索引优化" ID="ID_603256405" CREATED="1694700168339" MODIFIED="1694700172722"/>
<node TEXT="查询优化" ID="ID_1589354060" CREATED="1694700172918" MODIFIED="1694700180001"/>
<node TEXT="库表结构优化" ID="ID_1633639225" CREATED="1694700180321" MODIFIED="1694700186064"/>
</node>
<node TEXT="锁" ID="ID_1949514260" CREATED="1694604060393" MODIFIED="1694604076761"/>
<node TEXT="事务" ID="ID_1336816404" CREATED="1694604077026" MODIFIED="1694604082722"/>
<node TEXT="日志" ID="ID_1434750860" CREATED="1694604085538" MODIFIED="1694604088513"/>
<node TEXT="存储结构" ID="ID_959020719" CREATED="1694604091006" MODIFIED="1694604100561"/>
</node>
<node TEXT="Sqlite" POSITION="top_or_left" ID="ID_1387552309" CREATED="1694526295007" MODIFIED="1694611037895"/>
<node TEXT="Redis" ID="ID_845189570" CREATED="1694526642067" MODIFIED="1694611039013"/>
</node>
<node TEXT="网络编程" FOLDED="true" POSITION="bottom_or_right" ID="ID_1808192869" CREATED="1694526653455" MODIFIED="1694526657688">
<edge COLOR="#ff0000"/>
<node TEXT="Netty" POSITION="bottom_or_right" ID="ID_299099951" CREATED="1694525901443" MODIFIED="1694526661005"/>
</node>
<node TEXT="Web服务器" FOLDED="true" POSITION="bottom_or_right" ID="ID_1272302502" CREATED="1694526662586" MODIFIED="1694527020419">
<edge COLOR="#0000ff"/>
<node TEXT="Nginx" POSITION="bottom_or_right" ID="ID_234215185" CREATED="1694526289679" MODIFIED="1694526669201"/>
<node TEXT="Tomcat" ID="ID_1088761317" CREATED="1694526672913" MODIFIED="1694526677359"/>
</node>
<node TEXT="前端" FOLDED="true" POSITION="bottom_or_right" ID="ID_1827196028" CREATED="1694526679789" MODIFIED="1694526692223">
<edge COLOR="#00ff00"/>
<node TEXT="JavaFX" POSITION="bottom_or_right" ID="ID_1054595644" CREATED="1694525969234" MODIFIED="1694526695659"/>
<node TEXT="Vue" ID="ID_430622207" CREATED="1694527004008" MODIFIED="1694527009093"/>
</node>
<node TEXT="工具" FOLDED="true" POSITION="bottom_or_right" ID="ID_291162627" CREATED="1694527035571" MODIFIED="1694527038168">
<edge COLOR="#ff00ff"/>
<node TEXT="Freemarker" ID="ID_1873531042" CREATED="1694527038560" MODIFIED="1694527042188"/>
<node TEXT="Log4j2" ID="ID_146282114" CREATED="1694527054034" MODIFIED="1694527058276"/>
</node>
</node>
</map>
