package com.mapherdf.maplenilset.data.color

import kotlin.math.sqrt

data class ColorNameEntry(
    val chinese: String,
    val english: String,
)

object ColorName {

    private data class NamedColor(val r: Int, val g: Int, val b: Int, val entry: ColorNameEntry)

    private val namedColors = listOf(
        NamedColor(255, 0, 0, ColorNameEntry("纯红", "Red")),
        NamedColor(255, 99, 71, ColorNameEntry("番茄红", "Tomato")),
        NamedColor(255, 69, 0, ColorNameEntry("橙红", "Orange red")),
        NamedColor(255, 140, 0, ColorNameEntry("暗橙", "Dark orange")),
        NamedColor(255, 165, 0, ColorNameEntry("橙色", "Orange")),
        NamedColor(255, 215, 0, ColorNameEntry("金色", "Gold")),
        NamedColor(255, 193, 7, ColorNameEntry("琥珀", "Amber")),
        NamedColor(255, 255, 0, ColorNameEntry("纯黄", "Yellow")),
        NamedColor(154, 205, 50, ColorNameEntry("黄绿", "Yellow green")),
        NamedColor(173, 255, 47, ColorNameEntry("绿黄", "Green yellow")),
        NamedColor(124, 252, 0, ColorNameEntry("草绿", "Lawn green")),
        NamedColor(0, 255, 0, ColorNameEntry("纯绿", "Lime green")),
        NamedColor(50, 205, 50, ColorNameEntry("石灰绿", "Lime")),
        NamedColor(34, 139, 34, ColorNameEntry("森林绿", "Forest green")),
        NamedColor(0, 128, 0, ColorNameEntry("纯绿", "Green")),
        NamedColor(0, 100, 0, ColorNameEntry("暗绿", "Dark green")),
        NamedColor(46, 139, 87, ColorNameEntry("海绿", "Sea green")),
        NamedColor(60, 179, 113, ColorNameEntry("中海绿", "Medium sea green")),
        NamedColor(32, 178, 170, ColorNameEntry("浅海绿", "Light sea green")),
        NamedColor(0, 128, 128, ColorNameEntry("青绿", "Teal")),
        NamedColor(0, 206, 209, ColorNameEntry("暗绿松石", "Dark turquoise")),
        NamedColor(64, 224, 208, ColorNameEntry("绿松石", "Turquoise")),
        NamedColor(127, 255, 212, ColorNameEntry("碧绿", "Aquamarine")),
        NamedColor(0, 255, 255, ColorNameEntry("青色", "Cyan")),
        NamedColor(176, 224, 230, ColorNameEntry("粉蓝", "Powder blue")),
        NamedColor(135, 206, 250, ColorNameEntry("浅天蓝", "Light sky blue")),
        NamedColor(135, 206, 235, ColorNameEntry("天蓝", "Sky blue")),
        NamedColor(173, 216, 230, ColorNameEntry("浅蓝", "Light blue")),
        NamedColor(176, 196, 222, ColorNameEntry("亮钢蓝", "Light steel blue")),
        NamedColor(119, 136, 153, ColorNameEntry("浅岩板灰", "Light slate gray")),
        NamedColor(112, 128, 144, ColorNameEntry("岩板灰", "Slate gray")),
        NamedColor(70, 130, 180, ColorNameEntry("钢蓝", "Steel blue")),
        NamedColor(100, 149, 237, ColorNameEntry("矢车菊蓝", "Cornflower blue")),
        NamedColor(65, 105, 225, ColorNameEntry("皇家蓝", "Royal blue")),
        NamedColor(0, 0, 255, ColorNameEntry("纯蓝", "Blue")),
        NamedColor(0, 0, 205, ColorNameEntry("中蓝", "Medium blue")),
        NamedColor(25, 25, 112, ColorNameEntry("午夜蓝", "Midnight blue")),
        NamedColor(0, 0, 139, ColorNameEntry("暗蓝", "Dark blue")),
        NamedColor(0, 0, 128, ColorNameEntry("海军蓝", "Navy")),
        NamedColor(123, 104, 238, ColorNameEntry("中岩板蓝", "Medium slate blue")),
        NamedColor(106, 90, 205, ColorNameEntry("岩板蓝", "Slate blue")),
        NamedColor(138, 43, 226, ColorNameEntry("蓝紫", "Blue violet")),
        NamedColor(148, 0, 211, ColorNameEntry("暗紫", "Dark violet")),
        NamedColor(75, 0, 130, ColorNameEntry("靛蓝", "Indigo")),
        NamedColor(147, 112, 219, ColorNameEntry("中紫", "Medium purple")),
        NamedColor(216, 191, 216, ColorNameEntry("蓟色", "Thistle")),
        NamedColor(221, 160, 221, ColorNameEntry("梅红", "Plum")),
        NamedColor(238, 130, 238, ColorNameEntry("紫罗兰", "Violet")),
        NamedColor(218, 112, 214, ColorNameEntry("兰花紫", "Orchid")),
        NamedColor(186, 85, 211, ColorNameEntry("中兰花紫", "Medium orchid")),
        NamedColor(186, 85, 211, ColorNameEntry("暗兰花紫", "Dark orchid")),
        NamedColor(153, 50, 204, ColorNameEntry("暗兰花", "Dark orchid")),
        NamedColor(128, 0, 128, ColorNameEntry("紫色", "Purple")),
        NamedColor(139, 0, 139, ColorNameEntry("暗洋红", "Dark magenta")),
        NamedColor(255, 0, 255, ColorNameEntry("洋红", "Magenta")),
        NamedColor(255, 20, 147, ColorNameEntry("深粉", "Deep pink")),
        NamedColor(255, 105, 180, ColorNameEntry("亮粉", "Hot pink")),
        NamedColor(255, 192, 203, ColorNameEntry("粉红", "Pink")),
        NamedColor(255, 182, 193, ColorNameEntry("浅粉红", "Light pink")),
        NamedColor(220, 20, 60, ColorNameEntry("猩红", "Crimson")),
        NamedColor(178, 34, 34, ColorNameEntry("耐火砖红", "Firebrick")),
        NamedColor(139, 0, 0, ColorNameEntry("暗红", "Dark red")),
        NamedColor(165, 42, 42, ColorNameEntry("棕色", "Brown")),
        NamedColor(205, 92, 92, ColorNameEntry("印度红", "Indian red")),
        NamedColor(240, 128, 128, ColorNameEntry("亮珊瑚红", "Light coral")),
        NamedColor(250, 128, 114, ColorNameEntry("鲑红", "Salmon")),
        NamedColor(233, 150, 122, ColorNameEntry("暗鲑红", "Dark salmon")),
        NamedColor(255, 160, 122, ColorNameEntry("亮鲑红", "Light salmon")),
        NamedColor(255, 127, 80, ColorNameEntry("珊瑚", "Coral")),
        NamedColor(210, 105, 30, ColorNameEntry("巧克力", "Chocolate")),
        NamedColor(160, 82, 45, ColorNameEntry("赭石", "Sienna")),
        NamedColor(139, 69, 19, ColorNameEntry("马鞍棕", "Saddle brown")),
        NamedColor(128, 0, 0, ColorNameEntry("栗色", "Maroon")),
        NamedColor(188, 143, 143, ColorNameEntry("玫瑰棕", "Rosy brown")),
        NamedColor(244, 164, 96, ColorNameEntry("沙棕", "Sandy brown")),
        NamedColor(210, 180, 140, ColorNameEntry("棕褐", "Tan")),
        NamedColor(245, 222, 179, ColorNameEntry("小麦", "Wheat")),
        NamedColor(245, 245, 220, ColorNameEntry("米色", "Beige")),
        NamedColor(240, 230, 140, ColorNameEntry("卡其", "Khaki")),
        NamedColor(255, 250, 205, ColorNameEntry("柠檬绸", "Lemon chiffon")),
        NamedColor(250, 250, 210, ColorNameEntry("浅金黄", "Light goldenrod")),
        NamedColor(255, 239, 213, ColorNameEntry("番木瓜", "Papaya whip")),
        NamedColor(255, 228, 196, ColorNameEntry("杏桃", "Peach puff")),
        NamedColor(255, 222, 173, ColorNameEntry("纳瓦白", "Navajo white")),
        NamedColor(255, 228, 225, ColorNameEntry("薄雾玫瑰", "Misty rose")),
        NamedColor(255, 240, 245, ColorNameEntry("薰衣草红", "Lavender blush")),
        NamedColor(250, 240, 230, ColorNameEntry("亚麻", "Linen")),
        NamedColor(253, 245, 230, ColorNameEntry("老蕾丝", "Old lace")),
        NamedColor(255, 248, 220, ColorNameEntry("玉米须", "Cornsilk")),
        NamedColor(255, 250, 240, ColorNameEntry("古典白", "Floral white")),
        NamedColor(248, 248, 255, ColorNameEntry("鬼白", "Ghost white")),
        NamedColor(240, 255, 240, ColorNameEntry("蜜瓜白", "Honeydew")),
        NamedColor(240, 255, 255, ColorNameEntry("青白", "Azure")),
        NamedColor(224, 255, 255, ColorNameEntry("浅青", "Light cyan")),
        NamedColor(240, 248, 255, ColorNameEntry("爱丽丝蓝", "Alice blue")),
        NamedColor(230, 230, 250, ColorNameEntry("薰衣草", "Lavender")),
        NamedColor(248, 248, 255, ColorNameEntry("幽灵白", "Ghost white")),
        NamedColor(255, 250, 250, ColorNameEntry("雪白", "Snow")),
        NamedColor(255, 255, 240, ColorNameEntry("象牙", "Ivory")),
        NamedColor(255, 255, 255, ColorNameEntry("纯白", "White")),
        NamedColor(0, 0, 0, ColorNameEntry("纯黑", "Black")),
        NamedColor(47, 79, 79, ColorNameEntry("暗岩灰", "Dark slate gray")),
        NamedColor(105, 105, 105, ColorNameEntry("暗灰", "Dim gray")),
        NamedColor(128, 128, 128, ColorNameEntry("灰色", "Gray")),
        NamedColor(169, 169, 169, ColorNameEntry("暗灰", "Dark gray")),
        NamedColor(192, 192, 192, ColorNameEntry("银色", "Silver")),
        NamedColor(211, 211, 211, ColorNameEntry("浅灰", "Light gray")),
        NamedColor(220, 220, 220, ColorNameEntry("淡灰", "Gainsboro")),
        NamedColor(245, 245, 245, ColorNameEntry("白烟", "White smoke")),
        NamedColor(216, 124, 95, ColorNameEntry("中等红", "Moderate red")),
        NamedColor(222, 184, 135, ColorNameEntry("硬木棕", "Burlywood")),
        NamedColor(255, 228, 181, ColorNameEntry("马科朗色", "Moccasin")),
        NamedColor(255, 218, 185, ColorNameEntry("桃色", "Peach")),
        NamedColor(255, 250, 205, ColorNameEntry("柠檬", "Lemon")),
        NamedColor(230, 230, 230, ColorNameEntry("银灰", "Silver gray")),
        NamedColor(250, 235, 215, ColorNameEntry("古董白", "Antique white")),
        NamedColor(255, 239, 213, ColorNameEntry("木瓜色", "Papaya")),
        NamedColor(255, 235, 205, ColorNameEntry("漂白杏仁", "Blanched almond")),
        NamedColor(255, 228, 196, ColorNameEntry("桃杏色", "Peach puff")),
    )

    private val exactNames = namedColors.associateBy({ named -> named.r * 65536 + named.g * 256 + named.b }) { it.entry }

    fun lookup(rgb: RgbColor): ColorNameEntry? {
        exactNames[rgb.r * 65536 + rgb.g * 256 + rgb.b]?.let { return it }
        var closest: ColorNameEntry? = null
        var closestDistance = Double.MAX_VALUE
        for (named in namedColors) {
            val dr = (named.r - rgb.r).toDouble()
            val dg = (named.g - rgb.g).toDouble()
            val db = (named.b - rgb.b).toDouble()
            val distance = sqrt((2.0 * dr) * (2.0 * dr) + (4.0 * dg) * (4.0 * dg) + (3.0 * db) * (3.0 * db))
            if (distance < closestDistance) {
                closestDistance = distance
                closest = named.entry
            }
        }
        return closest
    }
}
