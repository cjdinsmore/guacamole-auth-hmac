# guacamole-auth-hmac

Built for **Guacamole 0.9.12-incubating**.

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

Copy `guacamole-auth-hmac.jar` to the location specified by [`lib-directory`][config-classpath] in `guacamole.properties`.

This extension adds extra config keys to `guacamole.properties`:

| Variable                | Required | Default | Comments                                                                                                                 |
|-------------------------|----------|---------|--------------------------------------------------------------------------------------------------------------------------|
| `secret-key`            | yes      | None    | The key that will be used to verify URL signatures. Whatever is generating the signed URLs will need to share this value.|
| `timestamp-age-limit`   | no       | 600000  | A numeric value (in milliseconds) that determines how long a signed request should be valid for. 600000 ms = 10 min      |
| `use-local-privkey`     | no       | False   | A boolean value to specify whether or not Guacamole should check on the local filesystem for private keys.               |


[config-classpath]: http://guac-dev.org/doc/gug/configuring-guacamole.html#idp380240

## Usage

| Variable                | Required | Default | Comments                                                                                                                     |
|-------------------------|----------|---------|------------------------------------------------------------------------------------------------------------------------------|
| `id`                    | yes      | None    | A connection ID that must be unique per user session. Can be a random integer or UUID.                                       |
| `timestamp`             | yes      | None    | A unix timestamp in milliseconds. This is used to prevent replay attacks.                                                    |
| `guac.protocol`         | yes      | ssh     | One of `vnc` or `ssh`.                                                                                                       |
| `guac.hostname`         | yes      | None    | The hostname of the remote desktop server to connect to.                                                                     |
| `guac.port`             | yes      | None    | The port number to connect to.                                                                                               |
| `guac.username`         | no       | None    | Username to login with. If left blank, user will be prompted on SSH connections. VNC connections will fail.                  |
| `guac.password`         | no       | None    | Password to authenticate with. If left blank, user will be prompted on SSH connections. VNC connections will fail.           |
| `guac.*`                | no       | None    | Any other configuration parameters recognized by Guacamole can be by prefixing them with `guac.`.                            |
| `signature`             | yes      | None    | The SHA256 encrypted concatenation of `timestamp`, `protocol`, `hostname`, `port`, `username`, `password` for authentication.|


#### Private keys
Since users are authenticated using a web request to the Guacamole server, it is insecure to use pubkey auth by sending the private keys over the web. This feature is enabled by the config parameter `use-local-privkey`. If true, Guacamole will look for the private key `$GUACAMOLE_HOME/keys/<username>/id_rsa_guac` and enable SFTP and use the key for SSH auth. The key and directory must be owned by the user running Guacamole (`tomcat7` in my case).

## Request Signing

Requests must be signed with an HMAC, where the message content is generated from the request parameters as follows:

 1. The parameters `timestamp`, `guac.protocol`, `hostname`, and `port` are concatenated. If `username` and `password` parameters are supplied, they are also concatenated.
 2. Encrypt using SHA256.

## POST
Using the python example, parameters can be POSTed to `/guacamole/api/tokens` to authenticate. The response is then sent as JSON and contains `authToken` which is then used to login: `guacamole/#/client/(connection)?token=(authToken)`

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
