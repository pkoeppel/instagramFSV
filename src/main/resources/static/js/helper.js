const MAX_FILE_SIZE_MB = 10; // Max. Dateigröße in MB
const MAX_FILE_SIZE = MAX_FILE_SIZE_MB * 1024 * 1024; // in Bytes
let matchData = [];

async function saveYouthMatchTemp(game) {

    let homeTeam = game.homeTeam.clubName;
    let awayTeam = game.awayTeam.clubName;
    const allTeams = await getAllTeams();
    if ((!allTeams.includes(homeTeam) || !allTeams.includes(homeTeam)) && game.changedName == null) {
        alert("Spiel kann nicht zugeordnet werden. Bitte wähle ein passendes Hauptteam aus!");
    } else {
        game.team = document.getElementById('teamsSelect').value;
        matchData.push(game);
        let bufferedTeams = document.getElementById('bufferedTeams');
        let newItem = document.createElement('li');
        newItem.innerHTML = homeTeam + ' VS ' + awayTeam;
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

function addNewTeam(game) {
    if (game.homeTeam !== "FSV Treuen") {
        localStorage.setItem('newTeamName', game.homeTeam);
    } else {
        localStorage.setItem('newTeamName', game.awayTeam)
    }
    window.open('/pages/addTeam.html');
}

function showSaveTemp(match) {
    document.getElementById('created').innerHTML += match.team + '; ';
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
    let teamData = await getTeamData();
    for ([key, value] of Object.entries(teamData)) {
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

        if (value["lastLeagueMatchday"] != null) {
            let inputLeague = document.createElement("input");
            inputLeague.id = "inputLeague" + key;
            inputLeague.value = value["lastLeagueMatchday"];
            idFieldset.appendChild(inputLeague);
        }

        if (value["lastCupMatchday"] != null) {
            let inputCup = document.createElement("input");
            inputCup.id = "inputCup" + key;
            inputCup.value = value["lastCupMatchday"];
            idFieldset.appendChild(inputCup);
        }
        idFieldset.appendChild(document.createElement("br"));
    }
    let saveButton = document.createElement("input");
    saveButton.type = "button";
    saveButton.value = "Speichern";
    saveButton.addEventListener("click", function () {
        for ([key, value] of Object.entries(teamData)) {
            value["club-id"] = document.getElementById("inputId" + key).value;
            value["default-place"] = document.getElementById("inputPlace" + key).value;
            let league = document.getElementById("inputLeague" + key);
            let cup = document.getElementById("inputCup" + key);
            if (league != null) {
                value["lastLeagueMatchday"] = league.value;
            }
            if (cup != null) {
                value["lastCupMatchday"] = cup.value;
            }
        }
        updateTeamValues(teamData);
    })
    idFieldset.appendChild(saveButton);
}

async function getTeamIds(categories) {
    const teamIds = await getTeamData();
    let sortedList = Object.keys(teamIds).sort();
    let select = document.getElementById("teamsSelect");

    for (const key of sortedList) {
        let option = document.createElement("option");
        option.text = key;
        option.value = key;
        if (categories === "men") {
            if (key === "1" || key === "2") {
                select.append(option);
            }
        } else {
            if (key !== "1" && key !== "2") {
                select.append(option);
            }
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

function clearElement(element) {
    while (element.firstChild) {
        element.removeChild(element.firstChild);
    }
}

function createGameDetails(date, comp) {
    const details = document.createElement("h3");
    details.textContent = date + ' (' + comp + '):';
    return details;
}

function readGameData(game) {
    return {
        date: formatDate(game.gameDate),
        comp: game.competition,
        home: game.homeTeam,
        away: game.awayTeam
    }
}

function createTeamSelection(labelText, game, currentTeam, teamProperty) {
    const container = document.createDocumentFragment();

    container.appendChild(document.createElement("br"));

    const label = document.createElement("label");
    label.htmlFor = "mainTeams";
    label.textContent = `${labelText} wählen: `;
    container.appendChild(label);

    const input = document.createElement("input");
    input.setAttribute("list", "listMainTeams");
    input.name = "mainTeams";
    input.id = "mainTeams";

    const dataList = document.createElement("datalist");
    dataList.className = "clubs";
    dataList.id = "listMainTeams";
    dataList.name = "listMainTeams";
    fillDataList(dataList);

    input.addEventListener("change", () => {
        currentTeam.changedName = currentTeam.clubName;
        currentTeam.clubName = input.value;
        game[teamProperty] = currentTeam;
    });

    container.appendChild(input);
    container.appendChild(dataList);

    const addButton = document.createElement("input");
    addButton.type = "button";
    addButton.value = "Team hinzufügen";
    addButton.addEventListener("click", () => addNewTeam(game));
    container.appendChild(addButton);

    return container;
}

function createMatchdayInput(game) {
    const matchdayContainer = document.createDocumentFragment();

    matchdayContainer.appendChild(document.createElement("br"));

    const label = document.createElement("label");
    label.htmlFor = "matchday";
    label.textContent = "Spieltag: ";
    matchdayContainer.appendChild(label);

    const input = document.createElement("input");
    input.type = "text";
    input.id = "matchday";
    input.value = game.matchDay;
    input.addEventListener("change", () => game.matchDay = input.value);
    matchdayContainer.appendChild(input);

    return matchdayContainer;
}

function shouldAddMatchdayInput(type, competition) {
    const lowerComp = competition.toLowerCase();
    return type === "men" &&
        (lowerComp.includes("liga") ||
            lowerComp.includes("klasse") ||
            lowerComp.includes("pokal"));
}

function createTeamDetails(type, game, homeTeam, awayTeam, allTeams) {
    const teamsElement = document.createElement("p");
    teamsElement.className = "game";

    teamsElement.appendChild(document.createTextNode(`${homeTeam.clubName} vs ${awayTeam.clubName}`));
    if (shouldAddMatchdayInput(type, game.competition)) {
        teamsElement.appendChild(createMatchdayInput(game));
    }
    if (!allTeams.includes(homeTeam.clubName)) {
        teamsElement.appendChild(createTeamSelection("Heimhauptteam", game, homeTeam, "homeTeam"));
    }

    if (!allTeams.includes(awayTeam.clubName)) {
        teamsElement.appendChild(createTeamSelection("Auswärtshauptteam", game, awayTeam, "awayTeam"));
    }
    return teamsElement;
}

function createButton(text, onClick) {
    const button = document.createElement("button");
    button.innerText = text;
    button.addEventListener("click", onClick);
    return button;
}

function createActionButtons(type, game) {
    const container = document.createDocumentFragment();
    if (type === "men") {
        container.appendChild(createButton("Prematch", () => postMenMatch(game)));
    }
    if (type === "youth") {
        container.appendChild(createButton("Spiel vormerken", () => saveYouthMatchTemp(game)));
    }

    return container;
}

function createGameView(type, game, allTeams) {
    const gameView = document.createElement("div");
    const {date, comp, home, away} = readGameData(game);

    gameView.appendChild(createGameDetails(date, comp));
    gameView.appendChild(createTeamDetails(type, game, home, away, allTeams));
    gameView.appendChild(createActionButtons(type, game));
    return gameView;
}

async function showMatches(type, team) {
    const gamesContainer = document.getElementById("games");
    clearElement(gamesContainer);
    const allMatches = await getMatches();
    const allTeams = await getAllTeams();

    for (const game of allMatches[team]) {
        const gameView = createGameView(type, game, allTeams);
        gamesContainer.appendChild(gameView);
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

async function getMatches() {
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

async function getTeamData() {
    let ids;
    await fetch(window.location.origin + '/getTeamData')
        .then((response) => {
            return response.json();
        })
        .then((data) => {
            ids = data
        })
        .catch((error) => {
            alert("Es ist ein Fehler beim Laden aufgetreten: " + error);
            console.error('Error: ', error);
        });
    return ids;
}

function updateTeamValues(teamInfo) {
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
    game.team = document.getElementById('teamsSelect').value;
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