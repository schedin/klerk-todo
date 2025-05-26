package se.moshicon.klerkframework.todo_app.users

import dev.klerkframework.klerk.ActorIdentity

/**
 * Data carrier of an authenticated user populated with external groups (from an Identity Provider (IdP)).
 *
 * An instance has a reference to a User model in Klerk. The groups associated with the User is not stored in Klerk,
 * but is used for authorization inside Klerk.
 */
class GroupModelIdentity (
    val model: dev.klerkframework.klerk.Model<User>,
    val groups: List<String> = emptyList(),
) : ActorIdentity {
    override val type: Int = ActorIdentity.Companion.customType
    override val id: dev.klerkframework.klerk.ModelID<User> = model.id
    override val externalId: Long? = null
    override fun toString(): String = "model: $model groups: $groups"
}