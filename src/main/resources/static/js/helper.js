const MAX_FILE_SIZE_MB = 10; // Max. Dateigröße in MB
const MAX_FILE_SIZE = MAX_FILE_SIZE_MB * 1024 * 1024; // in Bytes
let matchData = [];

async function saveYouthMatchTemp(game) {

    let oppTeam = game.homeTeam;
    if (oppTeam === "FSV Treuen" || oppTeam.toLowerCase().includes("spg treuener land")) oppTeam = game.awayTeam;
    const allTeams = await getAllTeams();
    if (!allTeams.includes(oppTeam) && game.changedName == null) {
        alert("Spiel kann nicht zugeordnet werden. Bitte wähle ein passendes Hauptteam aus!");
    } else {
        let ownTeam = document.getElementById('youthTeamsSelect').value;
        game.youthTeam = ownTeam;
        matchData.push(game);
        let bufferedTeams = document.getElementById('bufferedTeams');
        let newItem = document.createElement('li');
        newItem.innerHTML = ownTeam + ' VS ' + oppTeam;
        bufferedTeams.appendChild(newItem);
    }
}

async function createMatchdayFile(game) {
    let oppTeam = game.homeTeam;
    let comp = game.competition;
    if (oppTeam === "FSV Treuen") oppTeam = game.awayTeam;
    const allTeams = await getAllTeams();
    if (!allTeams.includes(oppTeam)) {
        alert("Spiel kann nicht zugeordnet werden. Bitte wähle ein passendes Hauptteam aus!");
    } else {
        if (game.matchDay === "" && (comp.toLowerCase().includes("liga") || comp.toLowerCase().includes("klasse") || comp.toLowerCase().includes("pokal"))) {
            alert("Spiel muss einem Spieltag zugeordnet werden!");
        } else {
            postMenMatch(game);
        }
    }
}

function openKickoffPage(game) {
    localStorage.setItem('currentGame', JSON.stringify(game))
    window.open('/pages/men/kickoff-men.html');
}

function addNewTeam(game) {
    if (game.homeTeam !== "FSV Treuen") {
        localStorage.setItem('newTeamName', game.homeTeam);
    } else {
        localStorage.setItem('newTeamName', game.awayTeam)
    }
    window.open('/pages/addTeam.html');
}

function showSaveTemp(match) {
    document.getElementById('created').innerHTML += match.youth + '; ';
}

async function getAllTeamInformation() {
    let datalist = document.getElementById("clubs");
    const allTeams = await getAllTeams();
    allTeams.forEach(function (club) {
        let opt = document.createElement("option");
        opt.value = club;
        datalist.append(opt);
    })
    let idFieldset = document.getElementById("idField");
    let teamIds = await getTeamIds();
    for ([key, value] of Object.entries(teamIds)) {
        let lb = document.createElement("label");
        lb.innerText = key;
        idFieldset.appendChild(lb);
        let inputId = document.createElement("input");
        inputId.id = "inputId" + key;
        inputId.value = value["club-id"];
        idFieldset.appendChild(inputId);
        let inputPlace = document.createElement("input");
        inputPlace.id = "inputPlace" + key;
        inputPlace.value = value["default-place"];
        idFieldset.appendChild(inputPlace);
        idFieldset.appendChild(document.createElement("br"));
    }
    let saveButton = document.createElement("input");
    saveButton.type = "button";
    saveButton.value = "Speichern";
    saveButton.addEventListener("click", function () {
        for ([key, value] of Object.entries(teamIds)) {
            value["club-id"] = document.getElementById("inputId" + key).value;
            value["default-place"] = document.getElementById("inputPlace" + key).value;
        }
        updateTeamValues(teamIds);
    })
    idFieldset.appendChild(saveButton);
    let matchdayFieldset = document.getElementById("matchdayValues");
    let matchdays = await getSavedMatchdays();
    let lbLeague = document.createElement("label");
    lbLeague.innerText = "Liga:";
    matchdayFieldset.appendChild(lbLeague);
    let inputLeague = document.createElement("input");
    inputLeague.id = "inputLeague";
    inputLeague.value = matchdays["lastLeagueMatchday"];
    matchdayFieldset.appendChild(inputLeague);
    matchdayFieldset.appendChild(document.createElement("br"));
    let lbCup = document.createElement("label");
    lbCup.innerText = "Pokal:";
    matchdayFieldset.appendChild(lbCup);
    let inputCup = document.createElement("input");
    inputCup.id = "inputCup";
    inputCup.value = matchdays["lastCupMatchday"];
    matchdayFieldset.appendChild(inputCup);
    matchdayFieldset.appendChild(document.createElement("br"));
    let saveMatchdays = document.createElement("input");
    saveMatchdays.type = "button";
    saveMatchdays.value = "Speichern";
    saveMatchdays.addEventListener("click", function () {
        updateMatchdays(inputLeague.value, inputCup.value);
    })
    matchdayFieldset.appendChild(saveMatchdays);
}

async function getSavedMatchdays(){
    let matchdays;
    await fetch(window.location.origin + '/getSavedMatchdays')
        .then((response) => {
            return response.json();
        })
        .then((data) => { matchdays = data})
        .catch((error) => {
            alert("Es ist ein Fehler beim Laden aufgetreten: " + error);
            console.error('Error: ', error);
        });
    return matchdays;
}

function updateMatchdays(leagueMatchday, cupMatchday) {
    let formData = new FormData()
    formData.append("leagueMatchday", leagueMatchday);
    formData.append("cupMatchday", cupMatchday);
    fetch(window.location.origin + '/updateMatchdaysInfo', {
        method: 'POST',
        mode: 'cors',
        cache: 'no-cache',
        credentials: 'same-origin',
        redirect: 'follow',
        referrerPolicy: 'no-referrer',
        body: formData,
    })
        .then((response) => {
            alert("Erfolgreich gespeichert!");
            return response;
        })
        .catch((error) => {
            alert("Es ist ein Fehler beim Ändern aufgetreten: " + error);
            console.error('Error: ', error);
        });
}

async function getTeamIdsForYouth() {
    const teamIds = await getTeamIds();
    let sortedList = Object.keys(teamIds).sort();
    let select = document.getElementById("youthTeamsSelect");

    for (const key of sortedList) {
        let option = document.createElement("option");
        option.text = key;
        option.value = key;
        if (key !== "1") {
            select.append(option);
        }
    }
}

function formatDate(input) {
    const year = input.substring(0, 4);
    const month = input.substring(5, 7);
    const day = input.substring(8, 10);

    return `${day}.${month}.${year}`;
}

function setCharCount() {
    let charCount =
        document.getElementById('matchResult').value.length +
        document.getElementById('headline').value.length +
        document.getElementById('report').value.length +
        /**
         document.getElementById('reporterOpp').value.length +
         document.getElementById('reportOpp').value.length +
         document.getElementById('reporterOwn').value.length +
         document.getElementById('reportOwn').value.length +
         **/
        document.getElementById('future').value.length;
    document.getElementById('chars').innerHTML = "Zeichen: " + charCount + "/2200";
}

async function getAllMatches(type, team) {
    await fetch(window.location.origin + '/getNextMatches', {
        method: 'GET'
    })
        .then(r => r.json())
        .catch((error) => {
            alert("Es ist ein Fehler beim Laden aufgetreten: " + error);
            console.error('Error: ', error);
        });
    await showMatches(type, team);
}

async function showMatches(type, team) {
    let games = document.getElementById("games");
    while (games.firstChild) {
        games.removeChild(games.firstChild);
    }
    const allMatches = await getMatches()
    for (const game of allMatches[team]) {
        let gameView = document.createElement("div");
        let date = formatDate(game.gameDate);
        let comp = game.competition;
        let home = game.homeTeam;
        let away = game.awayTeam;
        let details = document.createElement("h3");
        details.textContent = date + ' (' + comp + '):';
        gameView.appendChild(details);
        let teams = document.createElement("p");
        teams.className = "game";
        teams.appendChild(document.createTextNode(home + ' vs ' + away));
        if (type === "men" && (comp.toLowerCase().includes("liga") || comp.toLowerCase().includes("klasse") || comp.toLowerCase().includes("pokal"))) {
            teams.appendChild(document.createElement("br"));
            let matchdayLb = document.createElement("label");
            matchdayLb.for = "matchday";
            matchdayLb.textContent = "Spieltag: ";
            teams.appendChild(matchdayLb);
            let matchday = document.createElement("input");
            matchday.type = "text";
            matchday.id = "matchday";
            matchday.value = game.matchDay;
            matchday.addEventListener("change", () => game.matchDay = matchday.value);
            teams.appendChild(matchday);
        }
        let teamName = game.homeTeam;
        if (teamName === "FSV Treuen" || teamName.toLowerCase().includes("spg treuener land")) teamName = game.awayTeam;
        const allTeams = await getAllTeams();
        if (!allTeams.includes(teamName)) {
            teams.appendChild(document.createElement("br"));
            let mainTeamLb = document.createElement("label");
            mainTeamLb.for = "mainTeams";
            mainTeamLb.textContent = "Hauptteam wählen: ";
            teams.appendChild(mainTeamLb);

            let mainTeams = document.createElement("input");
            mainTeams.setAttribute("list", "listMainTeams");
            mainTeams.name = "mainTeams";
            mainTeams.id = "mainTeams"

            let listMainTeams = document.createElement("datalist");
            listMainTeams.className = "clubs";
            listMainTeams.id = "listMainTeams";
            listMainTeams.name = "listMainTeams";
            fillDataList(listMainTeams);
            mainTeams.addEventListener("change", () => {
                if (game.homeTeam !== "FSV Treuen" && !game.homeTeam.toLowerCase().includes("spg treuener land")) {
                    game.changeName = game.homeTeam;
                    game.homeTeam = mainTeams.value;
                } else {
                    game.changeName = game.awayTeam;
                    game.awayTeam = mainTeams.value;
                }

            });
            teams.appendChild(mainTeams);
            teams.appendChild(listMainTeams);

            let btAddNew = document.createElement("input");
            btAddNew.type = "button";
            btAddNew.value = "Team hinzufügen";
            btAddNew.addEventListener("click", () => addNewTeam(game));
            teams.appendChild(btAddNew);
        }
        gameView.appendChild(teams);
        if (type === "men") {
            let btMatchday = document.createElement("button");
            btMatchday.innerText = "Spielankuendigung";
            btMatchday.addEventListener("click", () => createMatchdayFile(game));
            gameView.appendChild(btMatchday);
            let btKickoff = document.createElement("button");
            btKickoff.innerText = 'Kickoff';
            btKickoff.addEventListener("click", () => openKickoffPage(game));
            gameView.appendChild(btKickoff);
        }
        if (type === "youth") {
            let btSaveYouthMatch = document.createElement("button");
            btSaveYouthMatch.innerText = "Spiel vormerken";
            btSaveYouthMatch.addEventListener("click", () => saveYouthMatchTemp(game));
            gameView.appendChild(btSaveYouthMatch);
        }
        games.appendChild(gameView);
    }
}

async function fillDataList(datalist) {
    const allTeams = await getAllTeams()
    allTeams.forEach(function (club) {
        let opt = document.createElement("option");
        opt.value = club;
        datalist.append(opt);
    })
}

async function getMatches(){
    let allMatches;
    await fetch(window.location.origin + '/getMatches', {
        method: 'GET'
    })
        .then((result) => result.json())
        .then((data) => {
            allMatches = data;
        })
        .catch((error) => {
            alert("Es ist ein Fehler beim Laden aufgetreten: " + error);
            console.error('Error: ', error);
        });
    return allMatches;
}

async function getAllTeams() {
    let allTeams;
    await fetch(window.location.origin + '/getAllTeams')
        .then((response) => {
            return response.json();
        })
        .then((data) => {
            allTeams = data;
        })
        .catch((error) => {
            alert("Es ist ein Fehler beim Laden aufgetreten: " + error);
            console.error('Error: ', error);
        });
    return allTeams;
}

async function getTeamIds(){
    let ids;
    await fetch(window.location.origin + '/getTeamIds')
        .then((response) => {
            return response.json();
        })
        .then((data) => { ids = data})
        .catch((error) => {
            alert("Es ist ein Fehler beim Laden aufgetreten: " + error);
            console.error('Error: ', error);
        });
    return ids;
}

function updateTeamValues(teamInfo){
    let formData = new FormData();
    formData.append("newData", JSON.stringify(teamInfo));
    fetch(window.location.origin + '/updateTeamInfo', {
        method: 'POST',
        mode: 'cors',
        cache: 'no-cache',
        credentials: 'same-origin',
        redirect: 'follow',
        referrerPolicy: 'no-referrer',
        body: formData,
    })
        .then((response) => {
            alert("Erfolgreich gespeichert!");
            return response;
        })
        .catch((error) => {
            alert("Es ist ein Fehler beim Ändern aufgetreten: " + error);
            console.error('Error: ', error);
        });
}

function postMenMatch(game) {
    fetch(window.location.origin + '/postMatchMen', {
        method: 'POST',
        mode: 'cors',
        cache: 'no-cache',
        credentials: 'same-origin',
        headers: {'Content-Type': 'application/json',},
        redirect: 'follow',
        referrerPolicy: 'no-referrer',
        body: JSON.stringify(game)
    })
        .then(response => {
            let status = response.status;
            if (status === 200) {
                response.text().then(data => window.open(window.location.origin + '/download/' + data + '/Matchday.jpeg'))
            }
        })
        .catch((error) => {
            alert("Es ist ein Fehler beim Erstellen aufgetreten: " + error);
            console.error('Error: ', error);
        });
}

function postKickoffMen(match, coords, file) {
    let formData = new FormData();
    formData.append("match", match);
    formData.append("coords", coords);
    formData.append("file", file);
    fetch(window.location.origin + '/createKickoffMen', {
        method: 'POST',
        mode: 'cors',
        cache: 'no-cache',
        credentials: 'same-origin',
        redirect: 'follow',
        referrerPolicy: 'no-referrer',
        body: formData,
    })
        .then(response => response.text())
        .then((data) => {
            window.open(window.location.origin + '/download/' + data + '/Kickoff.jpeg');
        })
        .catch((error) => {
            alert("Es ist ein Fehler beim Erstellen aufgetreten: " + error);
            console.error('Error: ', error);
        });
}