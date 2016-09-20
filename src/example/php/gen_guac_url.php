
<?php

$username = $_GET['username'];
$hostname = $_GET['hostname'];
$protocol = $_GET['protocol'];

$vncPass = 'password';
$secretKey = 'secret';
$urlStart = 'http://localhost:8888/guacamole';

// $cmd = escapeshellcmd('/usr/local/tomcat7/.guacamole/gen_guac_url.py');
// $link = shell_exec($cmd);

$fields = array(
	'username' => urlencode($_POST['username']),
	'port' => urlencode($_POST['port']),
	'hostname' => urlencode($_POST['hostname']),
	'protocol' => urlencode($_POST['institution']),
);

foreach ( $fields as $key=>$value ) {
  $fields_string .=$key.'='.$value.'&';
}
rtrim ( $fields_string, '&' );

$ch = curl_init($url);

// Passing an array to CURLOPT_POSTFIELDS will encode the data
// as multipart/form-data, while passing a URL-encoded string
// will encode the data as application/x-www-form-urlencoded.

curl_setopt( $ch, CURLOPT_POST, count($fields) );
curl_setopt( $ch, CURLOPT_POSTFIELDS, $fields_string );

$result = curl_exec($ch);
curl_close($ch);

?>
