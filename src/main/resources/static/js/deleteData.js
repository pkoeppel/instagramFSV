function deleteClub() {
    let formData = new FormData();
    formData.append("club", document.getElementById('club').value);
    fetch(window.location.origin + '/deleteClub', {
        method: 'DELETE',
        mode: 'cors',
        cache: 'no-cache',
        credentials: 'same-origin',
        redirect: 'follow',
        referrerPolicy: 'no-referrer',
        body: formData,
    })
        .then(response => response.text())
        .catch((error) => {
            alert("Es ist ein Fehler beim Löschen aufgetreten: " + error);
            console.error('Error: ', error);
        });
}

