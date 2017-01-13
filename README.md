# guacamole-auth-hmac

## Description

This project is a plugin for [Guacamole](http://guac-dev.org), an HTML5 based
remote desktop solution supporting VNC/RFB, RDP, and SSH.

This plugin is an _authentication provider_ that enables stateless, on-the-fly
configuration of remote desktop connections that are authorized using a
pre-shared key. It is most appropriate for scenarios where you have an existing
user authentication & authorization mechanism.

## Building

guacamole-auth-hmac uses Maven for managing builds. After installing Maven you can build a
suitable jar for deployment with `mvn package`.

The resulting jar file will be placed in `target/guacamole-auth-hmac-<version>.jar`.

## Deployment & Configuration

**Warning** This plugin relies on API's introduced in Guacamole 0.8.3, so you must be running
at least that version before using this plugin.

Copy `guacamole-auth-hmac.jar` to the location specified by
[`lib-directory`][config-classpath] in `guacamole.properties`.

`guacamole-auth-hmac` adds two new config keys to `guacamole.properties`:

 * `secret-key` - The key that will be used to verify URL signatures.
    Whatever is generating the signed URLs will need to share this value.
 * `timestamp-age-limit` - A numeric value (in milliseconds) that determines how long
    a signed request should be valid for.


[config-classpath]: http://guac-dev.org/doc/gug/configuring-guacamole.html#idp380240

## Usage

 * `id`  - A connection ID that must be unique per user session. Can be a random integer ***or UUID***.
 * `timestamp` - A unix timestamp in milliseconds. This is used to prevent replay attacks.
 * `signature` - The SHA256 encrypted signature for authentication.
 * `guac.protocol` - One of `vnc` or `ssh`.
 * `guac.hostname` - The hostname of the remote desktop server to connect to.
 * `guac.port` - The port number to connect to.
 * `guac.username` - (_optional_)
 * `guac.password` - (_optional_)
 * `guac.*` - (_optional_) Any other configuration parameters recognized by
    Guacamole can be by prefixing them with `guac.`.

## Request Signing

Requests must be signed with an HMAC, where the message content is generated from the request parameters as follows:

 1. The parameters `timestamp`, `guac.protocol`, `hostname`, and `port` are concatenated.
 2. Encrypt using SHA256.

## POST
Using the php form example, parameters can be POSTed to `/guacamole/api/tokens` to authenticate. The response is then sent as JSON and contains `authToken` which is then used to login: `guacamole/#/client/(connection)?token=(authToken)`

`(connection)` is an encoded string that tells Guacamole to connect the user to a server. It is generated as follows:

1. Remove the first two characters from the ID (I have tried it without shortening the ID, but it did not work).
2. Append `NULLcNullhmac` to the shortened ID.
  - `NULL` represents a `NULL` character (often "\0").
  - `c` stands for connection.
  - `hmac` is the authentication provider.
3. Encode this with base64.

Then, add this to the URL after `guacamole/#/client/` and append the authToken parameter.

[More about using POST with Guacamole](https://glyptodon.org/jira/browse/GUAC-1102?jql=project%20%3D%20GUAC%20AND%20resolution%20%3D%20Unresolved%20AND%20priority%20%3D%20Major%20ORDER%20BY%20key%20DESC)

[Outline of how Guacamole receives and responds to authentication requests](https://sourceforge.net/p/guacamole/discussion/1110834/thread/8bea4c74/#102b)

[Explanation of the base64 encoded URL](https://sourceforge.net/p/guacamole/discussion/1110834/thread/fb609070/)
