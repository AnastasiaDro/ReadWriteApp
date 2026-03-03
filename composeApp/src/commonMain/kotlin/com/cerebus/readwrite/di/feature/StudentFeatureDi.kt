package com.cerebus.readwrite.di.feature

import com.cerebus.readwrite.view.ActiveStudentViewModel
import com.cerebus.readwrite.view.ChangeStudentViewModel
import com.cerebus.readwrite.view.CreateStudentViewModel
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

val studentFeatureModule = module {
    viewModelOf(::ActiveStudentViewModel)
    viewModelOf(::ChangeStudentViewModel)
    viewModelOf(::CreateStudentViewModel)
}
