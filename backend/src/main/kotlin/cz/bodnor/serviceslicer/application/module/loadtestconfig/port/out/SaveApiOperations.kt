package cz.bodnor.serviceslicer.application.module.loadtestconfig.port.out

import cz.bodnor.serviceslicer.domain.apiop.ApiOperation

interface SaveApiOperations {

    operator fun invoke(apiOperations: List<ApiOperation>)
}
