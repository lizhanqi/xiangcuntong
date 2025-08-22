package com.sk.weichat.ui.base

import android.os.Bundle
import androidx.viewbinding.ViewBinding
import java.lang.reflect.Method

/**
 * 基于ViewBinding的基础Activity类
 * 使用泛型自动返回对应的ViewBinding实例
 */
abstract class ViewBindingActivity<T : ViewBinding> : BaseActivity() {

    // 使用by lazy懒加载ViewBinding实例
    protected val binding: T by lazy {
        initViewBinding()
    }

    /**
     * 初始化ViewBinding
     * 通过反射调用对应布局的inflate方法
     */
    private fun initViewBinding(): T {
        try {
            // 获取当前类的泛型参数
            val superclass = javaClass.genericSuperclass
            val parameterizedType = superclass as? java.lang.reflect.ParameterizedType
                ?: throw IllegalStateException("Class must be parameterized with a ViewBinding type")
            // 获取泛型参数的实际类型（即具体的ViewBinding类）
            val bindingClass = parameterizedType.actualTypeArguments[0] as Class<T>
            // 获取inflate方法并调用
            val inflateMethod: Method = bindingClass.getMethod(
                "inflate", 
                android.view.LayoutInflater::class.java
            )
            
            return inflateMethod.invoke(null, layoutInflater) as T
        } catch (e: Exception) {
            throw RuntimeException("Failed to initialize ViewBinding", e)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // 设置布局内容为ViewBinding的根视图
        setContentView(binding.root)
    }
}