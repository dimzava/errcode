package errcode

import com.google.inject.AbstractModule
import com.google.inject.Scopes
import groovy.sql.Sql
import ratpack.groovy.sql.SqlProvider

class ErrcodeModule extends AbstractModule {

  @Override
  protected void configure() {
      bind(Sql).toProvider(SqlProvider)
      bind(LogFileService.class).in(Scopes.SINGLETON)
      bind(ErrorObjectService.class).in(Scopes.SINGLETON)
  }

}
