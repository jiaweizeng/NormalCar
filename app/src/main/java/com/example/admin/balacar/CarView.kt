package com.example.admin.balacar

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.util.AttributeSet
import android.view.SurfaceHolder
import android.view.SurfaceView

/**
 * Created by zjw on 2018/9/26.
 */
class CarView @JvmOverloads constructor(
        context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : SurfaceView(context, attrs, defStyleAttr), SurfaceHolder.Callback,Runnable {


    //当前线程的状态
    private var isThreadFlag: Boolean = false
    //声明一条线程
    private val mSurfaceThread by lazy {
        Thread(this)
    }
    // 获得画布对象，开始对画布画画
    private var mCanvas :Canvas?=null
    //当前布局状态
    private var mCurrentCartState = ISurfaceCartState.ready
    private var mSurfaceRunwayUtil:SurfaceRunwayUtil?=null
    private var mSurfaceCarUtil:SurfaceCartUtil?=null

    private val mPaint=Paint()



    init {
        holder.addCallback(this)
        isFocusable=true
        keepScreenOn=true
    }


    override fun surfaceChanged(holder: SurfaceHolder?, format: Int, width: Int, height: Int) {
        if (mSurfaceRunwayUtil==null)
        mSurfaceRunwayUtil=SurfaceRunwayUtil(context,width,height)
        if (mSurfaceCarUtil==null)
        mSurfaceCarUtil=SurfaceCartUtil(context,width,height)
    }

    override fun surfaceDestroyed(holder: SurfaceHolder?) {
        isThreadFlag=false
        holder?.removeCallback(this)
    }

    override fun surfaceCreated(holder: SurfaceHolder?) {
    }

    /**
     * 初始化绘制
     */
    fun restarDraw() {
        if (!isThreadFlag) {
            isThreadFlag = true
            mSurfaceThread.start()
        }
    }

    /**
     * 开始跑
     */
    fun start() {
        mCurrentCartState = ISurfaceCartState.run
    }

    fun close() {
        isThreadFlag = false
    }

    override fun run() {
        while (isThreadFlag) {
            val start = System.currentTimeMillis()
            try {
                synchronized(CarView::class.java) {
                    mCanvas=holder.lockCanvas()

                        //绘制背景
                        mSurfaceRunwayUtil?.draw(mCanvas!!, mCurrentCartState,mPaint)
                        //绘制赛车
                        mSurfaceCarUtil?.draw(mCanvas!!, mCurrentCartState,mPaint)


                }
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                if (mCanvas != null) {
                    holder.unlockCanvasAndPost(mCanvas)//结束锁定画图，并提交改变。
                }
            }
            val end = System.currentTimeMillis()
            // 让线程休息100毫秒
            if (end - start < 30) {
                try {
                    Thread.sleep(30 - (end - start))
                } catch (e: InterruptedException) {
                    e.printStackTrace()
                }

            }
        }

    }

    fun release() {
        holder.removeCallback(this)//界面销毁的时候注销回调
    }
}