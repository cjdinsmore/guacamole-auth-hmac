<?php
$username = 'username';
$hostname = 'hostname';
$protocol = 'vnc';
$port = 5901;
if ($protocol === 'ssh')
  $port = 22;
$id = rand( 1,20000);
$vncPass = 'password';
$secretKey = 'secret';
$timestamp = round(time() * 1000);
$message = "$timestamp$protocol";

$signature = hash_hmac('sha256', $message, $secretKey, 1);
?>

<!DOCTYPE html>
<meta charset='UTF-8'>
<html>
  <head>
    <title>Guacamole Button</title>
  </head>
  <body>
    <form enctype='application/x-www-form-urlencoded' method='POST' action='http://localhost:8888/guacamole/api/tokens' name='guacform'>

      <input type='hidden' name='timestamp'     value='<?= urlencode($timestamp) ?>'>
      <input type='hidden' name='guac.port'     value='<?= urlencode($port) ?>'>
      <input type='hidden' name='guac.password' value='<?= urlencode($vncPass) ?>'>
      <input type='hidden' name='guac.protocol' value='<?= urlencode($protocol) ?>'>
      <input type='hidden' name='signature'     value='<?= base64_encode($signature) ?>'>
      <input type='hidden' name='guac.hostname' value='<?= urlencode($hostname) ?>'>
      <input type='hidden' name='id'            value='<?= urlencode('c/'.$id) ?>'>

    </form>

    <div id="guac">
      <button type="button" onclick="submitForm()" >Connect to Guacamole!</button>
    </div>

    <script>
    function submitForm() {
      var xhr = new XMLHttpRequest();

      // Turn form into a string
      var str = $("form").serialize();

      // Open the xhr
      xhr.open('POST','http://localhost:8888/guacamole/api/tokens', true);
      xhr.setRequestHeader("Content-type","application/x-www-form-urlencoded");

      // Send the form data
      xhr.send(str);

      // Redirect on successful response
      xhr.onreadystatechange=function() {
        if(xhr.readyState===4) {
          if(xhr.status===200) {
            window.location.assign("http://localhost:8888/guacamole/#?token=" + JSON.parse(xhr.responseText).authToken);
          }
        }
      };
    }

    </script>

  </body>
</html>
