package kacper.mappingapp_engineersthesis

import android.content.Context
import android.graphics.*
import android.os.Environment
import androidx.preference.PreferenceManager
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStream
import java.lang.IllegalArgumentException


class DrawingClass(val context: Context, val filename: String) {

    private val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context)
    private val fillPaintColor = Color.RED
    private val backGroungPaint = Paint()
    private val fillPaint = Paint()
    private val linePaint = Paint()
    private val strokePaint = Paint()
    private val paintText = Paint(Paint.ANTI_ALIAS_FLAG)
    private val shakeThreshold = 10


    init {
        backGroungPaint.color = Color.rgb(230,230,230)
        backGroungPaint.style = Paint.Style.FILL
        fillPaint.style = Paint.Style.FILL
        fillPaint.color = fillPaintColor
        linePaint.style = Paint.Style.FILL
        linePaint.color = Color.BLACK
        strokePaint.style = Paint.Style.STROKE
        strokePaint.color = Color.BLACK
        strokePaint.strokeWidth = 10f
        paintText.color = Color.BLACK
        paintText.textSize = 15*sharedPreferences.getString("roomLength", "")!!.toInt()/200f
        paintText.setShadowLayer(1f, 0f, 1f, Color.WHITE)
    }

    fun getDataFromFile():MutableList<String> {
        val file = File(context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS), filename)
        val bufferedReader = file.bufferedReader()
        val lineList = mutableListOf<String>()
        bufferedReader.useLines { lines -> lines.forEach { lineList.add(it) } }
        return lineList
    }

    fun saveImageToFile(bitmap: Bitmap)
    {
        var bitmapFilename = File(filename).nameWithoutExtension
        bitmapFilename += ".jpg"

        var file = File(context.getExternalFilesDir(Environment.DIRECTORY_PICTURES), bitmapFilename)
        try {
            val outputStream: OutputStream = FileOutputStream(file)
            bitmap.compress(Bitmap.CompressFormat.JPEG,100, outputStream)
            outputStream.flush()
            outputStream.close()

        }catch (e:Exception){

        }
    }
    fun convertDataToMapOfList(lineList: MutableList<String>): Map<String, MutableList<out Any>>
    {

        var timeList = mutableListOf<Int>()
        var accXList = mutableListOf<Float>()
        var accYList = mutableListOf<Float>()
        var accZList = mutableListOf<Float>()
        var rotXList = mutableListOf<Int>()
        var before: String
        var after: String
        var dropAmount = 1000/sharedPreferences.getString("samplingFrequency", "")!!.toInt()
        if(dropAmount < 5) dropAmount = 5

        for (item in lineList.drop(dropAmount))//z
        {
            //time
            before = item.substringBefore(';')
            timeList.add(before.toInt())
            after = item.replace(',','.')
            after = after.substringAfter(';')
            //AccX
            before = after.substringBefore(';')
            accXList.add(before.toFloat())
            after = after.substringAfter(';')
            //AccY
            before = after.substringBefore(';')
            accYList.add(before.toFloat())
            after = after.substringAfter(';')
            //AccZ/
            before = after.substringBefore(';')
            accZList.add(before.toFloat())
            after = after.substringAfter(';')
            //RotX
            rotXList.add(after.toInt())

        }
        return mapOf("time" to timeList, "AccX" to accXList, "AccY" to accYList, "AccZ" to accZList, "RotX" to rotXList)
    }
    fun returnDirection(rotX: Int): Int{
        return when (rotX) {
            in -22..22 -> 0
            in 23..67 -> 1
            in 68..112 -> 2
            in 113..157 -> 3
            in 158..180 -> 4
            in -180..-158 -> 4
            in -157..-113 -> 5
            in -112..-68 -> 6
            in -67..-22 -> 7
            else -> throw IllegalArgumentException("Wrong Number in direction")
        }
    }


    fun measureDimensions(bitmap: Bitmap) : Bitmap{
        //górna i dolna krawedź
        var topPoint = Point(bitmap.width,bitmap.height)//jak najmiejsze współrzędne
        var bottomPoint = Point(0,0)//jak największe współrzędne
        var leftPoint = Point(bitmap.width,bitmap.height)
        var rightPoint = Point(0,0)
        for (x in 0 until bitmap.width)
        {
            for (y in 0 until bitmap.height){
                if (bitmap.getPixel(x,y) == fillPaintColor){
                    if (y < topPoint.y) topPoint = Point(x,y)
                    if (y > bottomPoint.y) bottomPoint = Point(x,y)
                    if (x < leftPoint.x) leftPoint = Point(x,y)
                    if (x > rightPoint.y) rightPoint = Point(x,y)
                }
            }
        }
        val measuredWidth = rightPoint.x - leftPoint.x + 1
        val measuredLength = bottomPoint.y - topPoint.y + 1//start from 0
        var dimensionBitmap = Bitmap.createBitmap(bitmap.width, 50, Bitmap.Config.ARGB_8888)
        var canvas = Canvas(dimensionBitmap)
        var text1 = "Measured Width: $measuredWidth"
        var text2 = "Measured Length: $measuredLength"
        val bounds = Rect()

        paintText.textSize = 20f
        canvas.drawRect(0f, 0f, dimensionBitmap.width*1f, dimensionBitmap.height*1f, backGroungPaint)
        paintText.getTextBounds(text1, 0, text1.length, bounds)
        canvas.drawText(text1, 10f, 20f, paintText)
        paintText.getTextBounds(text2, 0, text2.length, bounds)
        canvas.drawText(text2, 10f, 40f, paintText)
        var merged = Bitmap.createBitmap(bitmap.width, bitmap.height + dimensionBitmap.height, Bitmap.Config.ARGB_8888)
        canvas = Canvas(merged)
        canvas.drawBitmap(bitmap,0f,0f,null)
        canvas.drawBitmap(dimensionBitmap,0f,bitmap.height*1f,null)
        return merged
    }

    fun checkAcceleration(diffTime: Int, sumAcc: Float, sumLastAcc: Float) : Boolean{
        var speed: Float = Math.abs(sumAcc + sumLastAcc)/diffTime*10000
        return speed > shakeThreshold
    }

    fun returnBitmap(): Bitmap{

        val roomWidth = sharedPreferences.getString("roomWidth", "")!!.toInt()
        val roomLength = sharedPreferences.getString("roomLength", "")!!.toInt()
        val radius = sharedPreferences.getString("diameter", "")!!.toFloat()/2
        var speed: Int = sharedPreferences.getString("speed", "")!!.toInt()//Brać pod uwagę sampling frequency
        speed /= (1000/sharedPreferences.getString("samplingFrequency", "")!!.toInt())
        val lineList = getDataFromFile()

        var bitmap = Bitmap.createBitmap(2*roomWidth, 2*roomLength, Bitmap.Config.ARGB_8888)
        var canvas = Canvas(bitmap)

        val rectangle = Rect(0, 0, bitmap.width, bitmap.height)
        canvas.drawRect(rectangle, backGroungPaint)
        canvas.drawRect(rectangle, strokePaint)

        val dataMap = convertDataToMapOfList(lineList)
        val lastElement = dataMap["time"]?.last()
        //val lastElementTime = lastElement.toString().toInt()/1000

        val startX = roomWidth*1f
        val startY = roomLength*1f
        var lastLocationX = startX
        var lastLocationY = startY
        var sumAcc: Float
        var lastSumAcc = 0f
        var diffTime: Int
        var lastTime = 0
        var direction: Int
        canvas.drawCircle(startX, startY, radius, fillPaint)
        canvas.drawCircle(startX, startY, 1f, linePaint)
        var itemIndex = 0
        for (item in dataMap.getValue("time")){
            diffTime = item as Int - lastTime
            sumAcc = dataMap.getValue("AccX")[itemIndex].toString().toFloat() + dataMap.getValue("AccY")[itemIndex].toString().toFloat() + dataMap.getValue("AccZ")[itemIndex].toString().toFloat()
            direction = returnDirection(dataMap.getValue("RotX")[itemIndex].toString().toInt())
            if (checkAcceleration(diffTime, sumAcc, lastSumAcc)){
                when(direction){
                    0 ->{
                        canvas.drawCircle(lastLocationX, lastLocationY - speed, radius, fillPaint)
                        lastLocationY -= speed
                    } //N
                    1 -> {
                        canvas.drawCircle(lastLocationX + speed, lastLocationY - speed, radius, fillPaint)
                        lastLocationX += speed
                        lastLocationY -= speed
                    } //NE
                    2 -> {
                        canvas.drawCircle(lastLocationX + speed, lastLocationY, radius, fillPaint)
                        lastLocationX += speed
                    } //E
                    3 -> {
                        canvas.drawCircle(lastLocationX + speed, lastLocationY + speed, radius, fillPaint)
                        lastLocationX += speed
                        lastLocationY += speed
                    } //SE
                    4 -> {
                        canvas.drawCircle(lastLocationX, lastLocationY + speed, radius, fillPaint)
                        lastLocationY += speed
                    } //S
                    5 -> {
                        canvas.drawCircle(lastLocationX - speed, lastLocationY + speed, radius , fillPaint)
                        lastLocationX -= speed
                        lastLocationY += speed
                    } //SW
                    6 -> {
                        canvas.drawCircle(lastLocationX - speed, lastLocationY, radius, fillPaint)
                        lastLocationX -= speed
                    } //W
                    7 -> {
                        canvas.drawCircle(lastLocationX - speed, lastLocationY - speed, radius, fillPaint)
                        lastLocationX -= speed
                        lastLocationY -= speed
                    }//NW
                }
            }

            lastSumAcc = sumAcc
            lastTime = item
            itemIndex++
        }
        lastLocationX = startX
        lastLocationY = startY
        lastSumAcc = 0f
        lastTime = 0
        itemIndex = 0
        for (item in dataMap.getValue("time")){
            diffTime = item as Int - lastTime
            sumAcc = dataMap.getValue("AccX")[itemIndex].toString().toFloat() + dataMap.getValue("AccY")[itemIndex].toString().toFloat() + dataMap.getValue("AccZ")[itemIndex].toString().toFloat()
            direction = returnDirection(dataMap.getValue("RotX")[itemIndex].toString().toInt())
            if (checkAcceleration(diffTime, sumAcc, lastSumAcc)){
                when(direction){
                    0 ->{
                        canvas.drawCircle(lastLocationX, lastLocationY - speed, 1f, linePaint)
                        lastLocationY -= speed
                    } //N
                    1 -> {
                        canvas.drawCircle(lastLocationX + speed, lastLocationY - speed, 1f, linePaint)
                        lastLocationX += speed
                        lastLocationY -= speed
                    } //NE
                    2 -> {
                        canvas.drawCircle(lastLocationX + speed, lastLocationY, 1f, linePaint)
                        lastLocationX += speed
                    } //E
                    3 -> {
                        canvas.drawCircle(lastLocationX + speed, lastLocationY + speed, 1f, linePaint)
                        lastLocationX += speed
                        lastLocationY += speed
                    } //SE
                    4 -> {
                        canvas.drawCircle(lastLocationX, lastLocationY + speed, 1f, linePaint)
                        lastLocationY += speed
                    } //S
                    5 -> {
                        canvas.drawCircle(lastLocationX - speed, lastLocationY + speed, 1f , linePaint)
                        lastLocationX -= speed
                        lastLocationY += speed
                    } //SW
                    6 -> {
                        canvas.drawCircle(lastLocationX - speed, lastLocationY, 1f, linePaint)
                        lastLocationX -= speed
                    } //W
                    7 -> {
                        canvas.drawCircle(lastLocationX - speed, lastLocationY - speed, 1f, linePaint)
                        lastLocationX -= speed
                        lastLocationY -= speed
                    }//NW
                }
            }

            lastSumAcc = sumAcc
            lastTime = item
            itemIndex++
        }


/*        for (x in 0..lastElementTime){
            canvas.drawCircle(startX + x*speed, startY, diameter, fillPaint)
        }
        for (x in 0..lastElementTime*speed){
            canvas.drawCircle(startX + x, startY, 1f, linePaint)
        }
        paintText.getTextBounds(lastElementTime.toString(), 0, lastElementTime.toString().length, bounds)
        canvas.drawText(lastElementTime.toString(), (bitmap.width - bounds.width())/2f, bitmap.height - 100f, paintText);*/

        return bitmap
    }
}