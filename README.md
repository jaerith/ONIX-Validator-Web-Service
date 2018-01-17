# ONIX-Validator-Web-Service
This solution is a Java web app server that serves to provide a way for validating files of the ONIX XML format, which is the international standard for representing electronic data regarding books (along with other media).  This format has been established by the international book trade body known as EDITEUR.

In order to use this web service, you simply need to send a POST to something akin to the following:

http://localhost:8049/onix-validator

with the ONIX file's data as the content body of the request.  Since the ONIX format uses an external DTD or XSD file for validation (and since most files use an HTTP URL as the default reference), the first step of each request will replace the HTTP URL with a local one.  (These DTDs/XSDs should already be downloaded into a preset folder.)  A successful validation will return a HTTP code of 200, and an invalid body of ONIX data will return a value of 422, along with the error message.  Any other type of error will return a value of 400.

# NOTES

In order to download DTDs and XSDs that refer to the legacy versions of ONIX (i.e., 2.1 and below), you should go to EDITEUR's <a target="_blank" href="http://www.editeur.org/15/Archived-Previous-Releases/">download page for previous releases</a>.

In order to download DTDs and XSDs for the latest version of ONIX (i.e., 3.x), you should go to EDITEUR's <a target="_blank" href="http://www.editeur.org/93/Release-3.0-Downloads/">download page for the current release</a>.
