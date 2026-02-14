package kr.co.lokit.api.config.exposed

import kr.co.lokit.api.domain.map.application.port.MapQueryPort
import kr.co.lokit.api.domain.map.infrastructure.ExposedMapQueryAdapter
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.DatabaseConfig
import org.jetbrains.exposed.sql.transactions.TransactionManager
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import java.sql.Connection
import javax.sql.DataSource

@Configuration
class ExposedConfig {
    @Bean
    fun databaseConfig(): DatabaseConfig =
        DatabaseConfig {
            useNestedTransactions = false
        }

    @Bean
    fun exposedDatabase(
        dataSource: DataSource,
        databaseConfig: DatabaseConfig,
    ): Database {
        val database =
            Database.connect(
                datasource = dataSource,
                databaseConfig = databaseConfig,
            )
        TransactionManager.manager.defaultIsolationLevel =
            Connection.TRANSACTION_READ_COMMITTED
        return database
    }

    @Bean
    fun mapQueryPort(database: Database): MapQueryPort = ExposedMapQueryAdapter(database)
}
