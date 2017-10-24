# Play Sentry

A simple authentication and authorization library for [Play Framework Scala](https://www.playframework.com) 2.5.x and later.
___

Play Sentry is a fork of [play2-auth](https://github.com/t2v/play2-auth), redesigned to favor runtime dependency injection via Guice and to generally be more compatible with the latest versions of Play.

### Main Differences With play2-auth

#### Added
* Scaladoc and unit tests for all classes
* Supports runtime DI using Guice, mainly so that you can inject components into an `AuthConfig`
* Compatibility with Play 2.6.x
* `CookieTokenAccessor` parameters can be configured within `application.conf`

#### Removed
* No longer depends on [Stackable Controller](https://github.com/t2v/stackable-controller)
* play2-auth `ActionBuilder`s were completely replaced with `AuthenticatedActionBuilder` and `OptionalAuthenticatedActionBuilder`
* The social module is not supported
* Compile-time dependency injection via traits and self-types is not supported

## Versions

Use the following table to determine what Play Sentry version to use based on what versions of Scala and Play you are using:

| Play Version  | Play Sentry Version | Scala 2.11 | Scala 2.12 |
| --------------| ------------------- | ---------- | ---------- |
| 2.5.x         | 0.9.x               | &#10003;   |            |
| 2.6.x         | 1.0.x               | &#10003;   | &#10003;   |


## Installation

Add the following to your `build.sbt`:

```
libraryDependencies += "com.jaroop" %% "play-sentry" % "1.0.0"
libraryDependencies += "com.jaroop" %% "play-sentry-test" % "1.0.0" % "test"
```

## Setup

Integrating Play Sentry into your application is easy. Here is a simple example to illustrate the required steps:

```
case class Account(id: Long, email: String, role: Role)

sealed trait Role
case object Admin extends Role
case object Normal extends Role
case object Guest extends Role
```

### Implement an Environment Type

First, you need to extend the `Env` type and implement the `Id`, `User`, and `Authority` types to fit your application. Since all Play Sentry components are created via runtime dependency injection, we need a way for them to know what types they are working with. In play2-auth, the `User`, `Id` and `Authority` types were all defined in the `AuthConfig`. In Play Sentry, this is no longer possible, since the `AuthConfig` is created at runtime, and no longer has compile-time knowledge of your application's types. This is where the `Env` type comes in. `AuthConfig` (and other compoments) are parameterized around the `Env` type, so that when you inject `AuthConfig[EnvImpl]` into one of your components, the `AuthConfig` will be correctly wired with your application's types.

Here is an implementation for our example:

```
trait EnvImpl extends Env {

    /** The type of the user's ID in your application. */
    type Id = Long
    
    /** The type of the user object in your application. */
    type User = Account
    
    /** The authority type (determines access to resources) in your application. */
    type Authority = Role
}
```

### Implement an AuthConfig

Next, you implement an `AuthConfig` to wire up Play Sentry with your application's user authentication, authorization, and related behavior. If you've previously used play2-auth, you will find the `AuthConfig` here to be almost exactly the same, except that some of the methods are no longer necessary or have moved to `Env`.

```
class SimpleAuthConfig @Inject() (
    accountService: AccountService
) extends AuthConfig[EnvImpl] {

    /** How long a session lasts before it expires. */
    def sessionTimeout = 1.hour

    /** Finds a user by ID. Alter this method to find your own user type by ID. */
    def resolveUser(id: Long)(implicit context: ExecutionContext): Future[Option[User]] =
        Future.successful(accountService.findById(id))

    /** Where to redirect the user after a successful login. */
    def loginSucceeded(request: RequestHeader)(implicit context: ExecutionContext): Future[Result] =
        Future.successful(Redirect("/home"))

    /** Where to redirect the user after they've been logged-out. */
    def logoutSucceeded(request: RequestHeader)(implicit context: ExecutionContext): Future[Result] =
        Future.successful(Redirect("/login"))

    /** If the user tries to access a resource that requires authentication, redirect them to login */
    def authenticationFailed(request: RequestHeader)(implicit context: ExecutionContext): Future[Result] =
        Future.successful(Redirect("/login")

    /** If the user tries to access a resource that requires authorization, but is not authorized, return Forbidden */
    def authorizationFailed(request: RequestHeader, user: User, authority: Option[Role])
        (implicit context: ExecutionContext): Future[Result] =
        Future.successful(Forbidden("Unauthorized."))

    /** A method that determines what authority a user has. Alter this method to fit your own authorization scheme. */
    def authorize(user: User, authority: Role)(implicit context: ExecutionContext): Future[Boolean] = Future.successful {
        (authority, user.role) match {
            case (_, Admin) => true
            case (Normal, Normal) => true
            case (Guest, Normal) => true
            case (Guest, Guest) => true
            case _ => false
        }       
    }

}
```

### Create a Module

The last thing you will need before you can use Play Sentry components within your application is a Guice `Module` that creates the required bindings. To maintain a degree of similarity with play2-auth (keeping the same abstract types), Play Sentry requires the usage of `TypeLiteral` bindings, which unfortunately does not allow us to use the Play module API, and instead we need to create a Guice `AbstractModule` directly.

```
import com.google.inject.{ AbstractModule, TypeLiteral }
import com.jaroop.play.sentry._
import scala.reflect.{ ClassTag, classTag }

class AuthModule extends AbstractModule {

    def configure(): Unit = {
        // Bind the AuthConfig to your own implementation
        bind(new TypeLiteral[AuthConfig[EnvImpl]]() {}).to(classOf[SimpleAuthConfig])

        // Default binding for TokenAccessor
        bind(classOf[TokenAccessor]).to(classOf[CookieTokenAccessor])

        // Bind IdContainer (with your Id type) to CacheIdContainer (recommended)
        bind(new TypeLiteral[IdContainer[Long]] {}).to(new TypeLiteral[CacheIdContainer[Long]] {})

        // Bind an instance of ClassTag[Id] (required for CacheIdContainer)
        bind(new TypeLiteral[ClassTag[Long]] {}).toInstance(classTag[Long])
    }

}
```

### Enable the Module

Then, simply enable the module in your `application.conf`:

```
play.modules.enabled += "com.example.AuthModule"

```

Note: If you are using `CacheIdContainer`, you will also need to enable a cache module such as EhCache or Memcached (recommended for distributed environments).

## Usage

Once configured, using Play Sentry is as simple as injecting the components you need into a controller and calling the desired method. 

### Logging In

To allow a user to log in, inject the `Login` class into your controller, filling in the type parameter with your own `Env` type. Then, call `gotoLoginSucceeded(userId)` from your action.

```
class Application @Inject() (
    login: Login[EnvImpl],
    userService, UserService
)(implicit val ec: ExecutionContext) extends InjectedController {

    val loginForm = Form {
        tuple(
            "email" -> email,
            "password" -> nonEmptyText
        )
    }

    def authenticate = Action.async(parse.urlFormEncoded) { implicit request =>
        loginForm.bindFromRequest.fold(
            formWithErrors => Future.successful(views.html.login(formWithErrors, "Invalid email address or password.")),
            credentials => UserService.authenticate(credentials._1, credentials._2).map { user =>
                login.gotoLoginSucceeded(user.id)
            } getOrElse {
                // auth failed
            }
        )
    }
}
```

### Authenticated Actions

To require a user to be authenticated in order access a resource, you can use the `AuthenticatedActionBuilder`. Simply inject `AuthenticatedActionBuilder` into your controller, and use it like you would any other `Action`.

```
class HomeController @Inject() (
    action: AuthenticatedActionBuilder[EnvImpl]
)(implicit val ec: ExecutionContext) extends InjectedController {

    def home = action { implicit request =>
        Ok("If you can read this, you are logged-in!")
    }

}
```

### Accessing the Logged-In User

All authenticated and authorized actions receive of function block of `AuthRequest[A, User] => Result`, where the `AuthRequest` is a `WrappedRequest` that will contain the logged-in user. To access the logged-in user, you can call `request.user`.

### Authorized Actions

`AuthenticatedActionBuilder` can also produce actions that require authorization. To require that is user has authorization for a specific resource, call `withAuthorization` on the injected `AuthenticatedActionBuilder`, and provide the authority key to use.

```
class HomeController @Inject() (
    action: AuthenticatedActionBuilder[EnvImpl]
)(implicit val ec: ExecutionContext) extends InjectedController {

    def admin = action.withAuthorization(Admin) { implicit request =>
        Ok("If you can read this, you must be an Admin")
    }
    
}
```


### Optionally Authenticated Actions

You may also have actions that do not require authentication, but may alter their behavior depending on whether the user is logged-in or not. For such situations you can use `OptionalAuthenticatedActionBuilder`. It works similarly to `AuthenticatedActionBuilder`, except that it will invoke the action function for every user, and it uses  `OptionalAuthRequest` (instead of `AuthRequest`) where the user is an `Option[User]`.

```
class HomeController @Inject() (
    action: OptionalAuthenticatedActionBuilder[EnvImpl]
)(implicit val ec: ExecutionContext) extends InjectedController {

    def home = action { implicit request =>
        request.user match {
            case Some(user) => Ok(s"You are logged-in as ${user.email}.")
            case None => Ok("You are not logged-in.")
        }
    }
    
}
```

### Logging Out

Similar to logging in, in order to log a user out of your application, you can inject `Logout` into your controller, then call `gotoLogoutSucceeded` to destroy the logged-in user's session.

```
class Appplication @Inject() (
    logout: Logout[EnvImpl]
)(implicit val ec: ExecutionContext) extends InjectedController {

    def logout() = Action.async { implicit request =>
        logout.gotoLogoutSucceeded()
    }
    
}
```

## Testing

Play Sentry comes with a small testing library to help you test your controllers that rely on Sentry action builders.

First, there is the preferred way in which you can use `MockAuthenticationActionBuilder` or `MockOptionalAuthenticatedActionBuilder`. In both cases, you provide a user (or optional user) that the action builder will return statically. That is, both of these mock action builders will always authenticate and authorize the user, and `request.user` will always be the value you've provided. For example, let's say we want to test the `index` method of this controller:

```
@Singleton
class HomeController @Inject() (
    authenticatedAction: AuthenticatedActionBuilder[EnvImpl]
)(implicit val ec: ExecutionContext) extends InjectedController {

    def secretPage = authenticatedAction { implicit request =>
        Ok(views.html.index(s"Success! You are logged in as ${request.user}"))
    }
}

```

In order to test this controller, we need to create an instance of `HomeController` that mocks out `AuthenticatedActionBuilder`. We can do so like this (using specs2):

```
import com.jaroop.play.sentry.test._

"show the correct message" in {
    val user = ... // The user that should be logged-in
    val builder = MockAuthenticatedActionBuilder(user)
    val controller = new HomeController(builder)
    val request = FakeRequest()
    val result = controller.secretPage(request)
    contentAsString(result) must contain(...)
}
```

There is also a method included in the `test` package that is very similar to `withLoggedIn` from play2-auth. You can use it in cases where you want a certain request to come from a particular logged-in user, and you want to test your controller method through the router (note, like other parts of the library, this method only works with Guice). We can write the same test from above as follows:

```
import com.jaroop.play.sentry.test._

"show the correct message" in new WithApplication {
    val request = FakeRequest(GET, "/secret").withLoggedIn[EnvImpl](1L) // specify your Env type and the logged-in user's ID
    val Some(result) = route(app, request)
    contentAsString(result) must contain(...)
}
```

## Running the Example Project

Play Sentry comes with a very simple example project that walks through usage the basic features in a functional application.

To obtain the examples project, first clone the repository:

```
git clone git@github.com:jaroop/play-sentry
```

Then run the example project with sbt:

```
sbt examples/run
```

## License

Play Sentry is distributed under the [Apache License, Version 2.0](http://www.apache.org/licenses/LICENSE-2.0.html)
