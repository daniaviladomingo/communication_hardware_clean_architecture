package communication.hardware.clean.ui

import communication.hardware.clean.SingleLiveEvent
import communication.hardware.clean.base.BaseViewModel
import communication.hardware.clean.domain.camera.model.Picture
import communication.hardware.clean.domain.interactor.type.CompletableUseCase
import communication.hardware.clean.domain.interactor.type.CompletableUseCaseWithParameter
import communication.hardware.clean.domain.interactor.type.ObservableUseCase
import communication.hardware.clean.domain.interactor.type.SingleUseCase
import communication.hardware.clean.domain.location.model.Location
import communication.hardware.clean.domain.model.mapper.Mapper
import communication.hardware.clean.domain.sms.model.Sms
import communication.hardware.clean.schedulers.IScheduleProvider
import communication.hardware.clean.ui.data.Resource

class MainActivityViewModel(
    private val getLocationUseCase: SingleUseCase<Location>,
    private val getLocationsUseCase: ObservableUseCase<Location>,
    private val stopLocationsUseCase: CompletableUseCase,
    private val getSmsUseCase: SingleUseCase<Sms>,
    private val sendSmsUseCase: CompletableUseCaseWithParameter<Sms>,
    private val shakinUseCase: ObservableUseCase<Unit>,
    private val takePictureUseCase: SingleUseCase<Picture>,
    private val scheduleProvider: IScheduleProvider,
    private val shakeMapper: Mapper<Unit, String>
) : BaseViewModel() {

    val locationUseCaseLiveData = SingleLiveEvent<Resource<Location>>()
    val smsUseCaseLiveData = SingleLiveEvent<Resource<Sms>>()
    val shakeLiveData = SingleLiveEvent<Resource<String>>()
    var takePictureLiveData = SingleLiveEvent<Resource<Picture>>()

    fun getLocation() {
        locationUseCaseLiveData.value = Resource.loading()
        addDisposable(getLocationUseCase.execute()
            .observeOn(scheduleProvider.ui())
            .subscribeOn(scheduleProvider.io())
            .subscribe({ location ->
                locationUseCaseLiveData.value = Resource.success(location)
            }) {
                locationUseCaseLiveData.value = Resource.error(it.localizedMessage)
            })
    }

    fun getLocations() {
        locationUseCaseLiveData.value = Resource.loading()
        addDisposable(getLocationsUseCase.execute()
            .observeOn(scheduleProvider.ui())
            .subscribeOn(scheduleProvider.io())
            .subscribe({ location ->
                locationUseCaseLiveData.value = Resource.success(location)
            }) {
                locationUseCaseLiveData.value = Resource.error(it.localizedMessage)
            })
    }

    fun stopLocations() {
        addDisposable(stopLocationsUseCase.execute()
            .observeOn(scheduleProvider.ui())
            .subscribeOn(scheduleProvider.io())
            .subscribe({}) {})
    }

    fun getSms() {
        smsUseCaseLiveData.value = Resource.loading()
        addDisposable(getSmsUseCase.execute()
            .observeOn(scheduleProvider.ui())
            .subscribeOn(scheduleProvider.io())
            .subscribe({ sms ->
                smsUseCaseLiveData.value = Resource.success(sms)
            }) {
                smsUseCaseLiveData.value = Resource.error(it.localizedMessage)
            })
    }

    fun sendSms(sms: Sms) {
        smsUseCaseLiveData.value = Resource.loading()
        addDisposable(sendSmsUseCase.execute(sms)
            .observeOn(scheduleProvider.ui())
            .subscribeOn(scheduleProvider.io())
            .subscribe({
                smsUseCaseLiveData.value = Resource.success(sms)
            }) {
                smsUseCaseLiveData.value = Resource.error(it.localizedMessage)
            })
    }

    fun isShaking() {
        addDisposable(shakinUseCase.execute()
            .observeOn(scheduleProvider.ui())
            .subscribeOn(scheduleProvider.io())
            .subscribe({
                shakeLiveData.value = Resource.success(shakeMapper.map(Unit))
            }) {
                shakeLiveData.value = Resource.error(it.localizedMessage)
            })
    }

    fun takePicture() {
        takePictureLiveData.value = Resource.loading()
        addDisposable(takePictureUseCase.execute()
            .subscribe({ picture ->
                takePictureLiveData.value = Resource.success(picture)
            }) {
                takePictureLiveData.value = Resource.error(it.localizedMessage)
            })
    }
}