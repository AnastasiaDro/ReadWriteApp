package com.cerebus.readwrite.di.feature

import com.cerebus.data.studyprogress.data.StudyProgressRepositoryImpl
import com.cerebus.data.studyprogress.data.storage.StudyProgressStorage
import com.cerebus.data.studyprogress.data.storage.StudyProgressStorageImpl
import com.cerebus.data.studyprogress.domain.repositories.StudyProgressRepository
import org.koin.dsl.module

val studyProgressModule = module {
    single<StudyProgressRepository> {
        StudyProgressRepositoryImpl(storage = get())
    }

    single<StudyProgressStorage> {
        StudyProgressStorageImpl(dao = get())
    }
}
