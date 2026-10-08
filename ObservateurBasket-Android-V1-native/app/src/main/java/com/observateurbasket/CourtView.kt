package com.observateurbasket

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View

data class Marker(var label:String, var x:Float, var y:Float)

class CourtView(context: Context, attrs: AttributeSet?=null): View(context, attrs) {
    val markers = mutableListOf<Marker>()
    private var selected: Marker? = null
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    override fun onDraw(c: Canvas) {
        super.onDraw(c)
        paint.style=Paint.Style.FILL
        paint.color=Color.rgb(236,245,235)
        c.drawRect(0f,0f,width.toFloat(),height.toFloat(),paint)
        paint.style=Paint.Style.STROKE
        paint.strokeWidth=3f
        paint.color=Color.DKGRAY
        c.drawRect(10f,10f,width-10f,height-10f,paint)
        c.drawLine(width/2f,10f,width/2f,height-10f,paint)
        c.drawCircle(width/2f,height/2f,35f,paint)
        c.drawRect(10f,height/2f-55f,85f,height/2f+55f,paint)
        c.drawRect(width-85f,height/2f-55f,width-10f,height/2f+55f,paint)
        paint.style=Paint.Style.FILL
        paint.textAlign=Paint.Align.CENTER
        paint.textSize=30f
        for(m in markers){
            paint.color=if(m.label.startsWith("A")) Color.rgb(30,90,200) else if(m.label.startsWith("B")) Color.rgb(210,50,50) else Color.rgb(30,30,30)
            c.drawCircle(m.x,m.y,22f,paint)
            paint.color=Color.WHITE; paint.textSize=13f
            c.drawText(m.label,m.x,m.y+5f,paint)
        }
    }
    override fun onTouchEvent(e: MotionEvent): Boolean {
        when(e.action){
            MotionEvent.ACTION_DOWN -> {
                selected = markers.minByOrNull { (it.x-e.x)*(it.x-e.x)+(it.y-e.y)*(it.y-e.y) }
                    ?.takeIf { (it.x-e.x)*(it.x-e.x)+(it.y-e.y)*(it.y-e.y) < 1600 }
                if(selected==null) { markers.add(Marker("R",e.x,e.y)); invalidate() }
                return true
            }
            MotionEvent.ACTION_MOVE -> { selected?.let { it.x=e.x; it.y=e.y; invalidate() }; return true }
            MotionEvent.ACTION_UP -> { selected=null; return true }
        }
        return true
    }
    fun add(label:String){
        markers.add(Marker(label,width/2f,height/2f)); invalidate()
    }
    fun clear(){ markers.clear(); invalidate() }
}
