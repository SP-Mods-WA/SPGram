package com.spmods.spgram.presentation.settings.profile

import com.arkivanov.decompose.value.Value
import com.spmods.spgram.domain.models.BirthdateModel
import com.spmods.spgram.domain.models.BusinessOpeningHoursModel
import com.spmods.spgram.domain.models.ChatModel
import com.spmods.spgram.domain.models.UserModel

interface EditProfileComponent {
    val state: Value<State>

    fun onBack()
    fun onUpdateFirstName(firstName: String)
    fun onUpdateLastName(lastName: String)
    fun onUpdateBio(bio: String)
    fun onUpdateUsername(username: String)
    fun onUpdateBirthdate(birthdate: BirthdateModel?)
    fun onUpdatePersonalChatId(chatId: Long)
    fun onUpdateBusinessBio(bio: String)
    fun onUpdateBusinessAddress(address: String, latitude: Double = 0.0, longitude: Double = 0.0)
    fun onUpdateBusinessOpeningHours(openingHours: BusinessOpeningHoursModel?)
    fun onChangeAvatar(path: String)
    fun onSave()
    fun onReverseGeocode(lat: Double, lon: Double)
    fun onToggleUsername(username: String, active: Boolean)
    fun onReorderUsernames(usernames: List<String>)

    data class State(
        val user: UserModel? = null,
        val firstName: String = "",
        val lastName: String = "",
        val bio: String = "",
        val username: String = "",
        val birthdate: BirthdateModel? = null,
        val personalChatId: Long = 0L,
        val linkedChat: ChatModel? = null,
        val businessBio: String = "",
        val businessAddress: String = "",
        val businessLatitude: Double = 0.0,
        val businessLongitude: Double = 0.0,
        val businessOpeningHours: BusinessOpeningHoursModel? = null,
        val avatarPath: String? = null,
        val isLoading: Boolean = false,
        val error: String? = null,
        val showAvatarPicker: Boolean = false
    )

    fun onShowAvatarPicker(show: Boolean)
}
