package com.batch.study.job

import com.batch.study.listener.GLOBAL_CHUNK_SIZE
import com.batch.study.domain.payment.Payment
import com.batch.study.domain.payment.PaymentBackJpa
import com.batch.study.listener.JobDataSetUpListener
import com.batch.study.listener.JobReportListener
import org.springframework.batch.core.Job
import org.springframework.batch.core.Step
import org.springframework.batch.core.configuration.annotation.JobBuilderFactory
import org.springframework.batch.core.configuration.annotation.JobScope
import org.springframework.batch.core.configuration.annotation.StepBuilderFactory
import org.springframework.batch.core.launch.support.RunIdIncrementer
import org.springframework.batch.item.ItemProcessor
import org.springframework.batch.item.database.HibernateCursorItemReader
import org.springframework.batch.item.database.JpaItemWriter
import org.springframework.batch.item.database.JpaPagingItemReader
import org.springframework.batch.item.database.builder.JpaItemWriterBuilder
import org.springframework.batch.item.database.builder.JpaPagingItemReaderBuilder
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import javax.persistence.EntityManagerFactory

@Configuration
class JpaInsertJobConfiguration(
    private val jobBuilderFactory: JobBuilderFactory,
    private val jobDataSetUpListener: JobDataSetUpListener,
    entityManagerFactory: EntityManagerFactory
) {

    @Bean
    fun jpaInsertJob(
        jpaInsertStep: Step
    ): Job =
        jobBuilderFactory["jpaInsertJob"]
            .incrementer(RunIdIncrementer())
            .listener(JobReportListener())
            .listener(jobDataSetUpListener)
            .start(jpaInsertStep)
            .build()

    @Bean
    @JobScope
    fun jpaInsertStep(
        stepBuilderFactory: StepBuilderFactory,
        cursorItemReader: HibernateCursorItemReader<Payment>
    ): Step =
        stepBuilderFactory["jpaInsertStep"]
            .chunk<Payment, PaymentBackJpa>(GLOBAL_CHUNK_SIZE)
            .reader(cursorItemReader)
            .processor(processor)
            .writer(writer)
            .build()

    private val reader: JpaPagingItemReader<Payment> =
        JpaPagingItemReaderBuilder<Payment>()
            .queryString("SELECT p FROM Payment p")
            .entityManagerFactory(entityManagerFactory)
            .name("readerPayment")
            .build()

    private val processor: ItemProcessor<in Payment, out PaymentBackJpa> =
        ItemProcessor {
            PaymentBackJpa(it.amount, it.orderId)
        }

    private val writer: JpaItemWriter<PaymentBackJpa> =
        JpaItemWriterBuilder<PaymentBackJpa>()
            .entityManagerFactory(entityManagerFactory)
            .build()
}