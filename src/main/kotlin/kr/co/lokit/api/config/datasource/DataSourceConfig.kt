package kr.co.lokit.api.config.datasource

import org.springframework.beans.factory.config.BeanPostProcessor
import org.springframework.context.annotation.Configuration
import java.sql.Connection
import java.util.concurrent.Semaphore
import javax.sql.DataSource

@Configuration
class DataSourceConfig : BeanPostProcessor {

    override fun postProcessAfterInitialization(bean: Any, beanName: String): Any {
        if (bean is DataSource && bean !is SemaphoreDataSource) {
            return SemaphoreDataSource(bean, MAX_PERMITS)
        }
        return bean
    }

    companion object {
        private const val MAX_PERMITS = 20
    }
}

class SemaphoreDataSource(
    private val delegate: DataSource,
    maxPermits: Int,
) : DataSource by delegate {

    private val semaphore = Semaphore(maxPermits)

    override fun getConnection(): Connection {
        semaphore.acquire()
        return try {
            delegate.connection
        } catch (e: Exception) {
            semaphore.release()
            throw e
        }.let { SemaphoreConnection(it, semaphore) }
    }

    override fun getConnection(username: String?, password: String?): Connection {
        semaphore.acquire()
        return try {
            delegate.getConnection(username, password)
        } catch (e: Exception) {
            semaphore.release()
            throw e
        }.let { SemaphoreConnection(it, semaphore) }
    }
}

private class SemaphoreConnection(
    private val delegate: Connection,
    private val semaphore: Semaphore,
) : Connection by delegate {

    override fun close() {
        try {
            delegate.close()
        } finally {
            semaphore.release()
        }
    }
}
