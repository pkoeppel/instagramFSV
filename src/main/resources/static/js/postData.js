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
            .then(async response => {
                if (!response.ok) throw new Error(await response.text());
                invalidateClubCache();
                return response.text();
            })
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
        .then(async response => {
            if (!response.ok) throw new Error(await response.text());
            invalidateClubCache();
            return response.text();
        })
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
        headline: document.getElementById('headline').value,
        report: document.getElementById('report').value
    }
}

function postPictures() {
    if (!document.getElementById('matches').value) {
        alert("Bitte ein Spiel auswählen.");
        return;
    }
    let formData = new FormData();
    formData.append("match", JSON.stringify(getData()));
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

function postMenScore(type) {
    const match = document.getElementById('matches').value;
    const score = document.getElementById('score').value.trim();
    if (!match || !/^\d{1,2}:\d{1,2}$/.test(score)) {
        alert('Bitte den Spielstand im Format 0:0 eingeben.');
        return;
    }
    const formData = new FormData();
    formData.append('match', match);
    formData.append('score', score);
    fetch(window.location.origin + (type === 'halftime' ? '/postMenHalftime' : '/postMenFinish'), {
        method: 'POST',
        body: formData
    })
        .then(async response => {
            if (!response.ok) throw new Error(await response.text());
            return response.json();
        })
        .then(data => {
            document.getElementById('scoreInfo').textContent = 'Grafik erstellt.';
            window.open(window.location.origin + '/download/score/' + data.fileDir + '/' + data.fileName);
        })
        .catch(error => alert('Fehler beim Erstellen der Grafik: ' + error));
}