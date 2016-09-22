<?php

// $username = $_GET['username'];
// $hostname = $_GET['hostname'];
// $protocol = $_GET['protocol'];
$username = 'calvinmclean';
$hostname = 'hostname';
$protocol = 'vnc';

$port = 5901;
if ( $protocol === 'ssh' )
  $port=22;

$id = rand( 1, 20000 );
$vncPass = 'password';
$secretKey = 'secret';
$urlStart = 'http://localhost:8888/guacamole/#/client/' . $id . '?id=c%2F' . $id . '&';
$guacamole = 'http://localhost:8888/guacamole/#/';

$timestamp = time() * 1000;
$signature = base64_encode( hash_hmac( 'sha256', $timestamp . $protocol, $secretKey, 1 ) );

$fields = array(
  'id'            => urlencode($id),
  'timestamp'     => urlencode($timestamp),
	'guac.username' => urlencode($username),
	'guac.port'     => urlencode($port),
	'guac.hostname' => urlencode($hostname),
	'guac.protocol' => urlencode($protocol),
  'guac.password' => urlencode($vncPass),
  'signature'     => urlencode($signature)
);

foreach ( $fields as $key=>$value ) {
  $fields_string .= $key . '=' . $value . '&';
}

// Remove the last character, which is an extra '&'
$fields_string = substr($fields_string, 0, -1);
// print $fields_string . "\n\n";

$ch = curl_init($guacamole);

// Passing an array to CURLOPT_POSTFIELDS will encode the data
// as multipart/form-data, while passing a URL-encoded string
// will encode the data as application/x-www-form-urlencoded.

curl_setopt( $ch, CURLOPT_POST, 1 );
curl_setopt( $ch, CURLOPT_RETURNTRANSFER, 1 ); // curl_exec will output a string instead of direct HTML
curl_setopt( $ch, CURLOPT_POSTFIELDS, $fields_string );
curl_setopt( $ch, CURLOPT_FOLLOWLOCATION, 1 );
$result = curl_exec($ch); // outputs html as a string
// print_r(curl_getinfo($ch));
curl_close($ch);
// echo $result;
// header("Location: http://localhost:8888/guacamole");
?>
