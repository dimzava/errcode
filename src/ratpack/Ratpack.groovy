import errcode.ErrcodeModule
import errcode.ErrorObject
import errcode.ErrorObjectService
import errcode.LogFileService
import ratpack.hikari.HikariModule
import ratpack.jackson.JacksonModule

import static ratpack.groovy.Groovy.groovyTemplate
import static ratpack.groovy.Groovy.ratpack
import static ratpack.jackson.Jackson.json

ratpack {

    bindings {
        add new HikariModule()
        add new JacksonModule()
        add new ErrcodeModule()

        init { ErrorObjectService errorObjectService ->
            errorObjectService.createTablesMySql()
        }
    }

    handlers {

        prefix('logs') {
            get('all') { LogFileService logFileService ->
                blocking {
                    logFileService.all()
                } then {
                    render json(it)
                }
            }

            get('latest') { LogFileService logFileService ->
                blocking {
                    logFileService.latest()
                } then {
                    render json(it)
                }
            }

            get('yesterdays') { LogFileService logFileService ->
                blocking {
                    logFileService.yesterdays()
                } then {
                    render json(it)
                }
            }
        }

        prefix('errors') {
            get('list') { ErrorObjectService errorObjectService ->
                blocking {
                    errorObjectService.list()
                } then {
                    render json(it)
                }
            }

            get('count') { ErrorObjectService errorObjectService ->
                blocking {
                    errorObjectService.count()
                } then {
                    render json(it)
                }
            }

        }

        get(':errcode?') { ErrorObjectService errorObjectService ->
            String errcode = pathTokens["errcode"]
            blocking {
                errorObjectService.findByErrcode(errcode)
            } then { ErrorObject errObj ->
                Map model = [errcode: errcode]
                if(errObj) {
                    model['errObj'] = errObj
                }
                render groovyTemplate(model, "index.html")
            }
        }

    }

}
