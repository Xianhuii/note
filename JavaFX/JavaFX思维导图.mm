<map version="freeplane 1.11.5">
<!--To view this file, download free mind mapping software Freeplane from https://www.freeplane.org -->
<node TEXT="JavaFX" FOLDED="false" ID="ID_696401721" CREATED="1610381621824" MODIFIED="1693920837124" STYLE="oval">
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
<node TEXT="布局" POSITION="bottom_or_right" ID="ID_1007903589" CREATED="1693920829250" MODIFIED="1693920854252">
<edge COLOR="#ff0000"/>
<node TEXT="StackPane" ID="ID_137552312" CREATED="1693920854263" MODIFIED="1693920861619"/>
<node TEXT="AnchorPane" ID="ID_1801969673" CREATED="1693920862085" MODIFIED="1693920875219"/>
<node TEXT="GridPane" ID="ID_1245298581" CREATED="1693920865249" MODIFIED="1693920881735"/>
<node TEXT="FlowPane&amp;TilePane" ID="ID_1493014724" CREATED="1693920882436" MODIFIED="1693920898163"/>
<node TEXT="BorderPane" ID="ID_597500386" CREATED="1693920898429" MODIFIED="1693920902268"/>
<node TEXT="SpitPane" ID="ID_810819395" CREATED="1693920902629" MODIFIED="1693920904966"/>
<node TEXT="HBox&amp;VBox&amp;ButtonBar" ID="ID_1994938825" CREATED="1693920905188" MODIFIED="1693920919210"/>
</node>
<node TEXT="事件" POSITION="bottom_or_right" ID="ID_1178919836" CREATED="1693920921640" MODIFIED="1693921686852">
<edge COLOR="#0000ff"/>
<node TEXT="setOnXxx()" ID="ID_1163256980" CREATED="1693921242368" MODIFIED="1693921326451"/>
</node>
<node TEXT="动画" POSITION="bottom_or_right" ID="ID_47346214" CREATED="1693921328645" MODIFIED="1693921335246">
<edge COLOR="#00ff00"/>
<node TEXT="Animation" ID="ID_653288817" CREATED="1693921335251" MODIFIED="1693921394682"/>
</node>
<node TEXT="UI" POSITION="bottom_or_right" ID="ID_1321287455" CREATED="1693921405076" MODIFIED="1693921579653">
<edge COLOR="#ff00ff"/>
<node TEXT="样式：Paint" ID="ID_74496588" CREATED="1693921428289" MODIFIED="1693921575256"/>
<node TEXT="特效：Effect" ID="ID_1695914032" CREATED="1693921436913" MODIFIED="1693921529977"/>
</node>
<node TEXT="属性" POSITION="bottom_or_right" ID="ID_1403708470" CREATED="1693921672038" MODIFIED="1693921678060">
<edge COLOR="#00ffff"/>
<node TEXT="属性绑定" ID="ID_523822084" CREATED="1693921733479" MODIFIED="1693921744689"/>
</node>
</node>
</map>
