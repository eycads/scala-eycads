package com.eycads.sdk.protocols

import com.eycads.protocols.account.states.AccountEntityStateData
import com.eycads.protocols.sdk.uniqueConstraint.states.UniqueConstraintStateData
import com.eycads.protocols.user.states.UserEntityStateData

trait AccountState extends StateWithData
trait AccountCommand extends CommandWithReplyTo
trait CreateAccountCommand extends AccountCommand with CreateCommand
trait AccountEvent extends Event
trait AccountResponse extends Response
trait _AccountState extends AccountState
  with WithStateData[AccountEntityStateData]

trait UserState extends StateWithData
trait UserCommand extends CommandWithReplyTo
trait CreateUserCommand extends UserCommand with CreateCommand
trait UserEvent extends Event
trait _UserState extends UserState
  with WithStateData[UserEntityStateData]

trait UniqueConstraintState
  extends StateWithStateDataOnly[UniqueConstraintStateData]
trait UniqueConstraintCommand extends CommandWithReplyTo
trait UniqueConstraintEvent extends Event

trait RelationEvent extends Event
trait RelationCommand extends AccountCommand
  with UserCommand with UniqueConstraintCommand