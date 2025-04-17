// noinspection JSUnresolvedReference

let sendData = {
    team: null,
    matchType: null,
    matchDay: null,
    homeGame: null,
    ownStats: null,
    opponent: null,
    oppStats: null,
    matchDate: null,
    matchTime: null,
    homePlace: null,
    oppName: null
};

function sendMatch() {
    sendData.opponent = document.getElementById('opponent').value;
    sendData.homeGame = document.getElementById('homeGame').checked;
    sendData.matchDate = document.getElementById('kickoffDate').value;
    sendData.matchTime = document.getElementById('kickoffTime').value;
    if (document.getElementById('oppName').value !== "") {
        sendData.oppName = document.getElementById('oppName').value;
    } else {
	sendData.oppName = document.getElementById('opponent').value;
    }

    fetch(window.location.origin + '/createMatchMen', {
        method: 'POST',
        mode: 'cors',
        cache: 'no-cache',
        credentials: 'same-origin',
        headers: {'Content-Type': 'application/json',},
        redirect: 'follow',
        referrerPolicy: 'no-referrer',
        body: JSON.stringify(sendData),
    })
        .then(response => response.text())
        .then(data => {
            window.open(window.location.origin + '/download/' + data + '/Matchday.jpeg');
        })
        .catch((error) => {
            console.error('Error: ', error);
        });

}

function sendLeagueMatch() {
    if (checkInputs()) {
        let ownPoints = document.querySelector("#pointsSelf").value;
        let ownPlace = document.querySelector("#placeSelf").value;
        let ownGoals = document.querySelector("#goalsSelf").value;
        let ownAgainstGoals = document.querySelector("#goalsAgainstSelf").value;
        let ownForm = document.querySelector("#formSelf").value;
        let oppPoints = document.querySelector("#pointsOpponent").value;
        let oppPlace = document.querySelector("#placeOpponent").value;
        let oppGoals = document.querySelector("#goalsOpponent").value;
        let oppAgainstGoals = document.querySelector("#goalsAgainstOpponent").value;
        let oppForm = document.querySelector("#formOpponent").value;

        sendData.matchType = "leagueMatch";
        sendData.matchDay = document.getElementById('matchday').value;
        sendData.ownStats = "\nPlatz " + ownPlace + " (" + ownPoints + " / " + ownGoals + ":" + ownAgainstGoals + ")\nTrend: " + ownForm;
        sendData.oppStats = "\nPlatz " + oppPlace + " (" + oppPoints + " / " + oppGoals + ":" + oppAgainstGoals + ")\nTrend: " + oppForm;
        sendMatch();
    } else {
        alert("Bitte alle Felder ausfüllen");
    }


}

function sendCupMatch() {
    if (checkInputs()) {
        sendData.matchType = "cupMatch";
        sendData.matchDay = document.getElementById('cupround').value;
        sendMatch();
    } else {
        alert("Bitte alle Felder ausfüllen");
    }

}

function sendFriendMatch() {
    if (checkInputs()) {
        sendData.matchType = "friendMatch";
        sendMatch();
    } else {
        alert("Bitte alle Felder ausfüllen");
    }
}

function checkInputs() {
    let inputs = document.querySelectorAll('.mainpage input');
    for (let input of inputs) {
        if (input.value === '') {
            return false;
        }
    }
    return true;
}
