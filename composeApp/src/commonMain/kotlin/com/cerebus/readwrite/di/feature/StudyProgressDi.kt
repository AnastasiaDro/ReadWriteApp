package com.cerebus.readwrite.di.feature

import com.cerebus.core.game_engine.domain.repository.AtomicProgressLogRepository
import com.cerebus.core.game_engine.domain.repository.CardProgressRepository
import com.cerebus.core.game_engine.domain.repository.ReviewLogRepository
import com.cerebus.core.game_engine.domain.repository.StudentPrefsRepository
import com.cerebus.data.studyprogress.data.RoomCardProgressRepository
import com.cerebus.data.studyprogress.data.RoomReviewLogRepository
import com.cerebus.data.studyprogress.data.RoomStudentPrefsRepository
import org.koin.dsl.module

val studyProgressModule = module {
    single {
        RoomCardProgressRepository(dao = get())
    }
    single<CardProgressRepository> {
        get<RoomCardProgressRepository>()
    }
    single<AtomicProgressLogRepository> {
        get<RoomCardProgressRepository>()
    }

    single<ReviewLogRepository> {
        RoomReviewLogRepository(dao = get())
    }

    single<StudentPrefsRepository> {
        RoomStudentPrefsRepository(dao = get())
    }
}
