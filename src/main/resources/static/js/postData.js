function changeClub() {
    let newClubName = document.getElementById('newClubName').value
    if (newClubName !== "") {
        let formData = new FormData();
        formData.append("club", document.getElementById('club').value);
        formData.append("newClubName", newClubName);
        fetch(window.location.origin + '/updateClub', {
            method: 'POST',
            mode: 'cors',
            cache: 'no-cache',
            credentials: 'same-origin',
            redirect: 'follow',
            referrerPolicy: 'no-referrer',
            body: formData,
        })
            .then(response => response.text())
            .catch((error) => {
                alert("Es ist ein Fehler beim Ändern aufgetreten: " + error);
                console.error('Error: ', error);
            });
    } else {
        alert("Feld ist leer!");
    }
}

function postNewTeam() {
    //save file
    let formData = new FormData();
    formData.append("club", document.getElementById('clubName').value);
    formData.append("place", document.getElementById('matchPlace').value);
    formData.append("insta1", document.getElementById('clubInsta').value);
    formData.append("insta2", "");
    formData.append("file", document.getElementById('clubPic').files[0]);
    fetch(window.location.origin + '/postNewTeam', {
        method: 'POST',
        mode: 'cors',
        cache: 'no-cache',
        credentials: 'same-origin',
        redirect: 'follow',
        referrerPolicy: 'no-referrer',
        body: formData,
    })
        .then(response => response.text())
        .catch((error) => {
            alert("Es ist ein Fehler beim Erstellen aufgetreten: " + error);
            console.error('Error: ', error);
        });
}

function postYouthMatchday() {
    fetch(window.location.origin + '/postMatchFilesYouth', {
        method: 'POST',
        mode: 'cors',
        cache: 'no-cache',
        credentials: 'same-origin',
        headers: {'Content-Type': 'application/json',},
        redirect: 'follow',
        referrerPolicy: 'no-referrer',
        body: JSON.stringify(matchData)
    })
        .then(response => response.json())
        .then((data) => {
            for (let [key, value] of Object.entries(data)) {
                for (let i = 1; i <= value; i++) {
                    window.open(window.location.origin + '/download/youth/' + key + '/Matchday' + i + '.jpeg');
                }
            }
            console.log(data);
        })
        .catch((error) => {
            alert("Es ist ein Fehler beim Erstellen aufgetreten: " + error);
            console.error('Error: ', error);
        });
}

function getData() {
    return {
        match: document.getElementById('matches').value,
        result: document.getElementById('matchResult').value,
        headline: document.getElementById('headline').value,
        report: document.getElementById('report').value,
        /**
         reporterOpp: document.getElementById('reporterOpp').value,
         reportOpp: document.getElementById('reportOpp').value,
         reporterOwn: document.getElementById('reporterOwn').value,
         reportOwn : document.getElementById('reportOwn').value,
         **/
        future: document.getElementById('future').value
    }
}

function postPictures() {
    let formData = new FormData();
    formData.append("match",JSON.stringify(getData()));
    fetch(window.location.origin + '/postMenMatchResult', {
        method: 'POST',
        mode: 'cors',
        cache: 'no-cache',
        credentials: 'same-origin',
        redirect: 'follow',
        referrerPolicy: 'no-referrer',
        body: formData,
    })
        .then(response => response.json())
        .then(data => {
            let repDiv = document.getElementById('showReport');
            repDiv.innerText = data.caption;
            window.open(window.location.origin + '/zip-download/' + data.fileDir);
        })
        .catch((error) => {
            alert("Es ist ein Fehler beim Erstellen aufgetreten: " + error);
            console.error('Error: ', error);
        });
}