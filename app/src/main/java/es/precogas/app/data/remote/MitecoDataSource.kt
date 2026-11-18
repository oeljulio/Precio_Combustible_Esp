package es.precogas.app.data.remote

import es.precogas.app.data.model.MitecoProvinceData
import es.precogas.app.data.model.Province

interface MitecoDataSource {
    suspend fun fetchProvince(province: Province): MitecoProvinceData
}
