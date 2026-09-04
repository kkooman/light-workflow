package com.kkooman.lightworkflow.config;

import javax.sql.DataSource;
import com.zaxxer.hikari.HikariDataSource;
import org.apache.ibatis.session.SqlSessionFactory;
import org.mybatis.spring.SqlSessionFactoryBean;
import org.mybatis.spring.annotation.MapperScan;
import org.apache.ibatis.annotations.Mapper;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.jdbc.DataSourceProperties;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.transaction.PlatformTransactionManager;

@Configuration
@MapperScan(
        basePackages = "com.kkooman.lightworkflow.watchlist",
        sqlSessionFactoryRef = "watchlistSqlSessionFactory",
        annotationClass = Mapper.class)
public class DatabaseConfig {

    @Bean
    @Primary
    @ConfigurationProperties("spring.datasource.customer")
    public DataSourceProperties customerDataSourceProperties() {
        return new DataSourceProperties();
    }

    @Bean
    @Primary
    public DataSource customerDataSource(
            @Qualifier("customerDataSourceProperties") DataSourceProperties properties) {
        return utcDataSource(properties);
    }

    @Bean
    @ConfigurationProperties("spring.datasource.watchlist")
    public DataSourceProperties watchlistDataSourceProperties() {
        return new DataSourceProperties();
    }

    @Bean
    public DataSource watchlistDataSource(
            @Qualifier("watchlistDataSourceProperties") DataSourceProperties properties) {
        return utcDataSource(properties);
    }

    @Bean
    public SqlSessionFactory watchlistSqlSessionFactory(
            @Qualifier("watchlistDataSource") DataSource dataSource) throws Exception {
        SqlSessionFactoryBean factory = new SqlSessionFactoryBean();
        factory.setDataSource(dataSource);
        factory.setMapperLocations(new PathMatchingResourcePatternResolver()
                .getResources("classpath:/mapper/**/*.xml"));
        factory.setTypeAliasesPackage("com.kkooman.lightworkflow");
        org.apache.ibatis.session.Configuration configuration = new org.apache.ibatis.session.Configuration();
        configuration.setMapUnderscoreToCamelCase(true);
        factory.setConfiguration(configuration);
        return factory.getObject();
    }

    @Bean
    public PlatformTransactionManager watchlistTransactionManager(
            @Qualifier("watchlistDataSource") DataSource dataSource) {
        return new DataSourceTransactionManager(dataSource);
    }

    private DataSource utcDataSource(DataSourceProperties properties) {
        DataSource dataSource = properties.initializeDataSourceBuilder().build();
        if (dataSource instanceof HikariDataSource hikariDataSource) {
            hikariDataSource.setConnectionInitSql("SET TIME ZONE 'UTC'");
        }
        return dataSource;
    }
}
