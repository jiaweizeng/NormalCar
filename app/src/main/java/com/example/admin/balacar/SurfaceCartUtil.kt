package com.example.admin.balacar

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Paint
import com.ssf.tc.publish.activity.BitmapUtil
import java.util.*

/**
 * Created by zjw on 2018/9/26.
 */
class SurfaceCartUtil constructor(mContext: Context, private val mScreenWidth: Int,
                                 private val mScreenHeight: Int){

    //view的高度
    private val mHeight: Int=0
    //屏幕的宽度
    //偏移bitmap头部的距离
    private var mOffsetY: Int=0
    //跑车
    private val mCartIds = intArrayOf(R.drawable.c1, R.drawable.c2, R.drawable.c3, R.drawable.c4)
    private val mCartConfigs = ArrayList<CartConfig>()
    private val mFireBitmaps = arrayOfNulls<Bitmap>(3)
    //车的宽度
    private var mCartWidth: Float=0f
    //车的高度
    private var mCartHeight: Float=0f
    //喷火的高度
    private var mFirePosY: Float=0f
    //随机工具类
    private val mCartRandomUtil: CartRandomUtil
    //终点的结果列表
    private var mEndResultList: List<Int> = ArrayList()
    //已经完成
    private var mCallBack: CallBack? = null



    init {

        //偏移头部的数据
        mOffsetY = 135
        val options = BitmapFactory.Options()
        options.inPreferredConfig = Bitmap.Config.RGB_565//只有RGB，没有透明度
        //初始化赛车
        initCartBitmaps(mContext, mScreenWidth, options)
        //赛车的位置
        val cartBitmap = mCartConfigs[0].bitmap
        mCartWidth = cartBitmap.width.toFloat()
        mCartHeight=cartBitmap.height.toFloat()
        //喷火
        val fireResources = intArrayOf(R.drawable.ic_fire_01, R.drawable.ic_fire_02, R.drawable.ic_fire_03)
        for (i in fireResources.indices) {
//            mFireBitmaps[i] = BitmapUtil().scaleBitmap(BitmapFactory.decodeResource(mContext.resources, fireResources[i], options),70,250)
            mFireBitmaps[i] = BitmapFactory.decodeResource(mContext.resources, fireResources[i], options)
        }
        //火出现的位置
        val cartCenterY = (cartBitmap.height / 2.0).toFloat()
        val firCenterY = (mFireBitmaps[0]!!.height / 2.0).toFloat()
        mFirePosY = cartCenterY - firCenterY + 3
        //随机工具类
        mCartRandomUtil = CartRandomUtil(mScreenWidth,mScreenHeight, mCartWidth)

    }


    /**
     * 初始化赛车
     */
    private fun initCartBitmaps(context: Context, screenWidth: Int, options: BitmapFactory.Options) {
        val tilt = 12
        //车道大小
        val carWay = mScreenWidth/(2*mCartIds.size)
        for (i in mCartIds.indices) {
            val bitmap = BitmapUtil().scaleBitmap(BitmapFactory.decodeResource(context.resources, mCartIds[i], options),100,200)
//            val startX = screenWidth.toFloat() - bitmap!!.width * 1.35f - i * tilt
            val startX = (mScreenWidth/5+carWay*i+80).toFloat()
            val startY = (mScreenHeight-bitmap!!.height*2).toFloat()
            val cartConfig = CartConfig(i + 1, startX,startY, bitmap, null)
            mCartConfigs.add(cartConfig)
        }
    }


    /**
     * 绘制赛车
     */
    fun draw(canvas: Canvas, iCartState: ISurfaceCartState,paint: Paint) {
        val cartConfigs = mCartConfigs
        val size = mCartIds.size
        val offsetY = mOffsetY
        val height = (cartConfigs[0].bitmap.height / 0.78).toFloat()

        if (canvas!=null){
            when (iCartState) {
                ISurfaceCartState.ready ->
                    //准备中的赛车
                    drawReady(canvas, size, cartConfigs, height, offsetY,paint)
                else ->
                    //绘制赛车
                    drawRunway(canvas, size, cartConfigs, height, offsetY,paint)
            }
        }
    }

    //获取当前车的顺序集合
    private fun getCarOrder(cartConfigs: List<CartConfig>): IntArray {
        val currentMap = HashMap<Float, Int>(10)
        val currentX = FloatArray(10)
        for (i in cartConfigs.indices) {
            val curX = cartConfigs[i].curX
            currentMap[curX] = i
            currentX[i] = curX
        }
        Arrays.sort(currentX)
        val position = IntArray(10)
        for (i in 0 until mCartIds.size) {
            position[i] = currentMap[currentX[i]]!!
        }
        return position
    }

    /**
     * 绘制准备中
     */
    private fun drawReady(canvas: Canvas, size: Int, cartConfigs: List<CartConfig>, height: Float, offsetY: Int,paint: Paint) {

        //绘制赛车
        for (i in 0 until size) {
            val bean = cartConfigs[i]
            val bitmap = bean.bitmap
            //车子Y轴 重叠位置
            val y = i * height + offsetY
            //当前坐标
            val curX = bean.curX
            // 车子X轴
//            canvas.drawBitmap(bitmap, curX, y, paint)
            //每辆车的车道大小
            val carWay = mScreenWidth/5
            canvas.drawBitmap(bitmap, (carWay+carWay*i).toFloat(), (mScreenHeight-bean.bitmap.height).toFloat(), paint)
            //需要喷火
            val cartFire = getCartFire(bean)
            if (cartFire !== ICartFire.None) {
                val fireBitmap = getFireBitmap(cartFire)
                val width = bean.width
//                canvas.drawBitmap(fireBitmap, curX + width, y + mFirePosY, paint)
                canvas.drawBitmap(fireBitmap, curX +fireBitmap.width*2/3-15, y + mFirePosY+fireBitmap.height*2/3+20, paint)
            }
        }
    }

    /**
     * 绘制正在赛跑的过程
     */
    private fun drawRunway(canvas: Canvas, size: Int, cartConfigs: List<CartConfig>, height: Float, offsetY: Int,paint: Paint) {
        //游戏是否结束 有3 个 完成就 ok
        var isEndCount = cartConfigs.size
        //绘制赛车
        for (i in 0 until size) {
            val bean = cartConfigs[i]
            val bitmap = bean.bitmap
            //车子Y轴 重叠位置
//            val y = i * height + offsetY
            //当前坐标
            val curX = bean.curX
            val curY = bean.curY

            //结束的坐标
            asmEndX(bean)
            // 车子X轴
            var newCurX = curX
            // 车子Y轴
            var newCurY = curY
            //已经到达终点
            if (bean.speed != 0f) {
                isEndCount--
//                newCurX += bean.speed
                newCurY += bean.speed
            }
            // 更新最新的值
            bean.curX = newCurX
            bean.curY = newCurY
//            canvas.drawBitmap(bitmap, newCurX, y, paint)
            //每辆车的车道大小
            val carWay = mScreenWidth/4
//            canvas.drawBitmap(bitmap, (carWay+carWay*i).toFloat(), newCurY, paint)
            canvas.drawBitmap(bitmap, curX, newCurY, paint)
            //需要喷火
            val cartFire = getCartFire(bean)
            if (cartFire !== ICartFire.None) {
                val fireBitmap = getFireBitmap(cartFire)
//                val width = bean.width
//                canvas.drawBitmap(fireBitmap, newCurX + width, y + mFirePosY, null)
//                canvas.drawBitmap(fireBitmap, newCurX +fireBitmap.width*2/3-15, y + mFirePosY+fireBitmap.height*2/3+20, paint)
                canvas.drawBitmap(fireBitmap, newCurX +fireBitmap.width*2/3-15, newCurY + bean.height, paint)
            }
        }
        //完成回调
        if (isEndCount > 3 && mCallBack != null) {
            mCallBack?.finish()
            mCallBack = null
        }
    }

    /**
     * 指定一个终点
     */
    private fun getRangeEndX(bf: Float): Float {
        return (bf / 10.0 * (mScreenWidth - mCartWidth)).toFloat()
    }
    private fun getRangeEndY(bf: Float): Float {
        return (bf / 10.0 * (mScreenHeight - mCartHeight)).toFloat()
    }

    /**
     * 到达这个终点的最佳速度
     */
    private fun getOptimumSpeed(config: CartConfig): Float {
        val curX = config.curX
        val endX = config.endX
        //最佳速度
        var v = Math.abs(curX - endX) / mScreenWidth * 100
        if (v > 30) v = 30f
        return if (endX < curX) {
            -v
        } else {
            v
        }
    }
    private fun getOptimumSpeedY(config: CartConfig):Float{
        val curY = config.curY
        val endY = config.endY
        //最佳速度
        var v = Math.abs(curY - endY) / mScreenHeight * 100
        if (v > 30) v = 30f
        return if (endY < curY) {
            -v
        } else {
            v
        }
    }

    /**
     * 计算终点坐标
     */
    private fun asmEndX(bean: CartConfig) {
        val newEndX = bean.endX
        val newEndY = bean.endY
        if (newEndX == 0f) {
            val speed = mCartRandomUtil.random(bean)
            if (speed > 0) {
                //                KLog.e("号码：" + bean.getNumber() + " ->往后");
            } else {
                //                KLog.e("号码：" + bean.getNumber() + " ->往前");
            }
        } else {
            //还没有找到坐标
            if (mEndResultList.isEmpty()) {
                // -> 方向
                if (bean.speed > 0) {
                    if (bean.curX > newEndX) {
                        val speed = mCartRandomUtil.random(bean)
                        if (speed > 0) {
                            //                            KLog.e("号码：" + bean.getNumber() + " ->往后");
                        } else {
                            //                            KLog.e("号码：" + bean.getNumber() + " ->往前");
                        }
                    }
                } else {
                    if (bean.curX < newEndX) {
                        val speed = mCartRandomUtil.random(bean)
                        if (speed > 0) {
                            //                            KLog.e("号码：" + bean.getNumber() + " ->往后");
                        } else {
                            //                            KLog.e("号码：" + bean.getNumber() + " ->往前");
                        }
                    }
                }
            } else {
                if (!bean.isEnd) {
                    bean.isEnd = true
                    val position = mEndResultList.indexOf(bean.number) + 1
                    val rangeEndX = getRangeEndX(position.toFloat()) + mScreenWidth / 8.0f
                    bean.endX = rangeEndX
                    //最佳速度
                    val optimumSpeed = getOptimumSpeed(bean)
                    bean.speed = optimumSpeed
                } else {
                    //已经在终点了,设置为 0
                    val speed = bean.speed
                    if (speed != 0f) {
                        //还没有冲出屏幕
                        if (bean.curX > -mCartWidth) {
                            //到达终点
                            var isEnd = false
                            if (speed < 0 && bean.curX < bean.endX) {
                                isEnd = true
                            } else if (speed > 0 && bean.curX > bean.endX) {
                                isEnd = true
                            }
                            if (isEnd) {
                                //到终点
//                                KLog.e("号码：" + bean.getNumber() + " ->到终点")
                                //冲出屏幕
                                bean.speed = -10f
                                bean.endX = -mCartWidth
                            }
                        } else {
                            bean.speed = 0f
                        }

                    }
                }
            }

        }
        if (newEndY == 0f) {
            val speed = mCartRandomUtil.randomY(bean)
            if (speed > 0) {
                //                KLog.e("号码：" + bean.getNumber() + " ->往后");
            } else {
                //                KLog.e("号码：" + bean.getNumber() + " ->往前");
            }
        } else {
            //还没有找到坐标
            if (mEndResultList.isEmpty()) {
                // -> 方向
                if (bean.speed > 0) {
                    if (bean.curY > newEndY) {
                        val speed = mCartRandomUtil.randomY(bean)
                        if (speed > 0) {
                            //                            KLog.e("号码：" + bean.getNumber() + " ->往后");
                        } else {
                            //                            KLog.e("号码：" + bean.getNumber() + " ->往前");
                        }
                    }
                } else {
                    if (bean.curY < newEndY) {
                        val speed = mCartRandomUtil.randomY(bean)
                        if (speed > 0) {
                            //                            KLog.e("号码：" + bean.getNumber() + " ->往后");
                        } else {
                            //                            KLog.e("号码：" + bean.getNumber() + " ->往前");
                        }
                    }
                }
            } else {
                if (!bean.isEndY) {
                    bean.isEndY = true
                    val position = mEndResultList.indexOf(bean.number) + 1
                    val rangeEndY = getRangeEndY(position.toFloat()) + mScreenHeight / 8.0f
                    bean.endY = rangeEndY
                    //最佳速度
                    val optimumSpeedY = getOptimumSpeedY(bean)
                    bean.speed = optimumSpeedY
                } else {
                    //已经在终点了,设置为 0
                    val speed = bean.speed
                    if (speed != 0f) {
                        //还没有冲出屏幕
                        if (bean.curY > -mCartHeight) {
                            //到达终点
                            var isEnd = false
                            if (speed < 0 && bean.curY < bean.endY) {
                                isEnd = true
                            } else if (speed > 0 && bean.curY > bean.endY) {
                                isEnd = true
                            }
                            if (isEnd) {
                                //到终点
//                                KLog.e("号码：" + bean.getNumber() + " ->到终点")
                                //冲出屏幕
                                bean.speed = -10f
                                bean.endY = -mCartHeight
                            }
                        } else {
                            bean.speed = 0f
                        }

                    }
                }
            }

        }
    }

    /**
     * 是否需要喷火
     */
    private fun getCartFire(bean: CartConfig): ICartFire {
        return if (bean.speed > 0 || bean.curX < 0 || bean.curY<0) {
            //往后退,不需要喷火
            ICartFire.None
        } else {
            //当前坐标(600) - 终点坐标(200) = 400 /   屏幕 1200 / 6 == 200         400 / 200 = 2
            val dw = bean.nextFire()
            when {
                dw > 20 -> ICartFire.Large
                dw > 10 -> ICartFire.Middle
                else -> ICartFire.Small
            }

        }
    }

    /**
     * 获取喷火的绘制图
     */
    private fun getFireBitmap(cartFire: ICartFire): Bitmap {
        return when {
            cartFire === ICartFire.Large -> mFireBitmaps[0]!!
            cartFire === ICartFire.Middle -> mFireBitmaps[1]!!
            else -> mFireBitmaps[2]!!
        }
    }

    /**
     * 设置终点,得到终点后,指定加速
     */
    fun setEndResultList(endResultList: List<Int>, callBack: CallBack) {
        mEndResultList = endResultList
        mCallBack = callBack
    }

    /**
     * 重置
     */
    fun reset() {
        mEndResultList= ArrayList()
        val tilt = 12
        for (i in mCartConfigs.indices) {
            val startX = mScreenWidth.toFloat() - mCartWidth * 1.35f - i * tilt
            mCartConfigs[i].curX = startX
            mCartConfigs[i].endX = 0f
            mCartConfigs[i].speed = -2f

        }
    }


    /**
     * 完成的时候回调
     */
    interface CallBack {
        //完成
        fun finish()
    }
}