let matchData = [];
let dynamicFieldId = 0;
let settingsTeamData = {};
let matchesPromise = null;
let allTeamsPromise = null;
let teamDataPromise = null;

async function saveYouthMatchTemp(game) {

    let homeTeam = game.homeTeam.clubName;
    let awayTeam = game.awayTeam.clubName;
    const allTeams = await getAllTeams();
    if (!allTeams.includes(homeTeam) || !allTeams.includes(awayTeam)) {
        alert("Spiel kann nicht zugeordnet werden. Bitte wähle ein passendes Hauptteam aus!");
    } else {
        game.team = document.getElementById('teamsSelect').value;
        matchData.push(game);
        let bufferedTeams = document.getElementById('bufferedTeams');
        let newItem = document.createElement('li');
        newItem.textContent = homeTeam + ' VS ' + awayTeam;
        bufferedTeams.appendChild(newItem);
    }
}

function addNewTeam(game) {
    const clubName = game.homeTeam.clubName !== "FSV Treuen" ? game.homeTeam.clubName : game.awayTeam.clubName;
    localStorage.setItem('newTeamName', clubName);
    window.open('/pages/addTeam.html');
}

function showSaveTemp(match) {
    document.getElementById('created').textContent += match.team + '; ';
}

async function getAllTeamInformation() {
    const datalist = document.getElementById("clubs");
    const container = document.getElementById("idField");
    const [allTeams, teamData] = await Promise.all([getAllTeams(), getTeamData()]);
    settingsTeamData = teamData;
    const clubOptions = document.createDocumentFragment();
    allTeams.forEach(club => {
        const option = document.createElement("option");
        option.value = club;
        clubOptions.append(option);
    });
    datalist.replaceChildren(clubOptions);
    container.replaceChildren();
    const teamCards = document.createDocumentFragment();
    Object.entries(settingsTeamData)
        .sort(([left], [right]) => left.localeCompare(right, "de", {numeric: true}))
        .forEach(([key, value]) => {
            const card = document.createElement("article");
            card.className = "team-settings-card";
            const title = document.createElement("h3");
            const category = value["category"] || (key === "1" || key === "2" ? "men" : "youth");
            title.textContent = "Mannschaft " + key + " · " + (category === "men" ? "Herren" : "Kids");
            card.append(title);
            card.append(createTeamSettingField("Fussball.de-ID", "inputId" + key, value["club-id"]));
            card.append(createTeamSettingField("Standard-Spielort", "inputPlace" + key, value["default-place"]));
            if (value["lastLeagueMatchday"] != null) {
                card.append(createTeamSettingField("Nächster Liga-Spieltag", "inputLeague" + key, value["lastLeagueMatchday"], "number"));
            }
            if (value["lastCupMatchday"] != null) {
                card.append(createTeamSettingField("Nächste Pokalrunde", "inputCup" + key, value["lastCupMatchday"], "number"));
            }
            const deleteButton = document.createElement("button");
            deleteButton.type = "button";
            deleteButton.className = "team-delete-button settings-danger";
            deleteButton.textContent = "Mannschaft löschen";
            deleteButton.addEventListener("click", () => deleteTeamInformation(key));
            card.append(deleteButton);
            teamCards.append(card);
        });
    container.append(teamCards);

    const saveRow = document.createElement("div");
    saveRow.className = "settings-save-row";
    const saveButton = document.createElement("button");
    saveButton.type = "button";
    saveButton.className = "settings-primary";
    saveButton.textContent = "Mannschaftsdaten speichern";
    saveButton.addEventListener("click", () => {
        collectTeamSettings();
        updateTeamValues(settingsTeamData);
    });
    saveRow.append(saveButton);
    container.append(saveRow);
}

function createTeamSettingField(labelText, inputId, value, type = "text") {
    const field = document.createElement("div");
    field.className = "settings-field";
    const label = document.createElement("label");
    label.htmlFor = inputId;
    label.textContent = labelText;
    const input = document.createElement("input");
    input.id = inputId;
    input.type = type;
    input.value = value || "";
    if (type === "number") input.min = "1";
    field.append(label, input);
    return field;
}

function collectTeamSettings() {
    Object.entries(settingsTeamData).forEach(([key, value]) => {
        value["club-id"] = document.getElementById("inputId" + key).value.trim();
        value["default-place"] = document.getElementById("inputPlace" + key).value.trim();
        const league = document.getElementById("inputLeague" + key);
        const cup = document.getElementById("inputCup" + key);
        if (league != null) value["lastLeagueMatchday"] = league.value;
        if (cup != null) value["lastCupMatchday"] = cup.value;
    });
}

async function addTeamInformation() {
    const keyInput = document.getElementById("newTeamKey");
    const idInput = document.getElementById("newTeamClubId");
    const placeInput = document.getElementById("newTeamPlace");
    const typeInput = document.getElementById("newTeamType");
    const key = keyInput.value.trim().toUpperCase();
    const clubId = idInput.value.trim();
    const place = placeInput.value.trim();
    if (!/^[A-Z0-9_-]{1,10}$/.test(key)) {
        alert("Das Kürzel darf nur Buchstaben, Zahlen, - und _ enthalten.");
        return;
    }
    if (settingsTeamData[key] != null) {
        alert("Eine Mannschaft mit diesem Kürzel existiert bereits.");
        return;
    }
    if (clubId === "" || place === "") {
        alert("Fussball.de-ID und Standard-Spielort sind erforderlich.");
        return;
    }
    collectTeamSettings();
    const newTeam = {"club-id": clubId, "default-place": place, "category": typeInput.value};
    if (typeInput.value === "men") {
        newTeam["lastLeagueMatchday"] = document.getElementById("newTeamLeague").value || "1";
        newTeam["lastCupMatchday"] = document.getElementById("newTeamCup").value || "1";
    }
    settingsTeamData[key] = newTeam;
    if (await updateTeamValues(settingsTeamData)) {
        document.getElementById("newTeamForm").reset();
        await getAllTeamInformation();
    } else {
        delete settingsTeamData[key];
    }
}

async function deleteTeamInformation(key) {
    if (Object.keys(settingsTeamData).length <= 1) {
        alert("Die letzte Mannschaft kann nicht gelöscht werden.");
        return;
    }
    if (!confirm(`Mannschaft „${key}“ wirklich löschen?`)) return;
    collectTeamSettings();
    const deletedTeam = settingsTeamData[key];
    delete settingsTeamData[key];
    if (await updateTeamValues(settingsTeamData)) {
        await getAllTeamInformation();
    } else {
        settingsTeamData[key] = deletedTeam;
    }
}

async function getTeamIds(categories) {
    const teamIds = await getTeamData();
    let sortedList = Object.keys(teamIds).sort();
    const select = document.getElementById("teamsSelect");
    select.replaceChildren();

    for (const key of sortedList) {
        const option = document.createElement("option");
        option.text = key;
        option.value = key;
        const storedCategory = teamIds[key]["category"];
        const category = storedCategory || (key === "1" || key === "2" ? "men" : "youth");
        if (category === categories) {
            select.append(option);
        }
    }
    return select.value || null;
}

function formatDate(input) {
    const year = input.substring(0, 4);
    const month = input.substring(5, 7);
    const day = input.substring(8, 10);

    return `${day}.${month}.${year}`;
}

function setCharCount() {
    const headline = document.getElementById('headline').value.trim();
    const report = document.getElementById('report').value.trim();
    let fullReport = '';
    if (currentMenMatchDetails !== null) {
        fullReport = currentMenMatchDetails.matchLine.trim();
    }
    if (headline) {
        fullReport = fullReport ? fullReport + '\n\n' + headline : headline;
    }
    if (currentMenMatchDetails !== null) {
        if (currentMenMatchDetails.scorers && currentMenMatchDetails.scorers.trim()) {
            fullReport = fullReport ? fullReport + '\n\n' + currentMenMatchDetails.scorers.trim() : currentMenMatchDetails.scorers.trim();
        }
        if (currentMenMatchDetails.staticText) {
            fullReport = fullReport ? fullReport + '\n\n' + currentMenMatchDetails.staticText.trim() : currentMenMatchDetails.staticText.trim();
        }
    }
    if (report) {
        fullReport = fullReport ? fullReport + '\n\n' + report : report;
    }
    document.getElementById('chars').textContent = "Zeichen: " + Array.from(fullReport).length + "/2200";
}

function clearElement(element) {
    while (element.firstChild) {
        element.removeChild(element.firstChild);
    }
}

function createGameDetails(date, comp) {
    const details = document.createElement("h3");
    details.className = "game-card-title";
    details.textContent = date + ' · ' + comp;
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
    const container = document.createElement("div");
    container.className = "inline-form-group";
    const fieldId = "mainTeam-" + dynamicFieldId++;
    const listId = "mainTeamList-" + dynamicFieldId++;

    const label = document.createElement("label");
    label.htmlFor = fieldId;
    label.textContent = labelText;
    container.appendChild(label);

    const input = document.createElement("input");
    input.setAttribute("list", listId);
    input.name = "mainTeams";
    input.id = fieldId;

    const dataList = document.createElement("datalist");
    dataList.className = "clubs";
    dataList.id = listId;
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
    const matchdayContainer = document.createElement("div");
    matchdayContainer.className = "inline-form-group";
    const fieldId = "matchday-" + dynamicFieldId++;

    const label = document.createElement("label");
    label.htmlFor = fieldId;
    label.textContent = "Spieltag";
    matchdayContainer.appendChild(label);

    const input = document.createElement("input");
    input.type = "number";
    input.min = "1";
    input.id = fieldId;
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
    teamsElement.className = "game-teams";

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
    button.type = "button";
    button.className = "primary-action";
    button.innerText = text;
    button.addEventListener("click", onClick);
    return button;
}

function createActionButtons(type, game) {
    const container = document.createElement("div");
    container.className = "game-actions";
    if (type === "men") {
        container.appendChild(createButton("Prematch", () => postMenMatch(game)));
    }
    if (type === "youth") {
        container.appendChild(createButton("Spiel vormerken", () => saveYouthMatchTemp(game)));
    }

    return container;
}

function createGameView(type, game, allTeams) {
    const gameView = document.createElement("article");
    gameView.className = "game-card";
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

    const fragment = document.createDocumentFragment();
    for (const game of allMatches[team] || []) {
        fragment.appendChild(createGameView(type, game, allTeams));
    }
    gamesContainer.appendChild(fragment);
}

async function fillDataList(datalist) {
    const allTeams = await getAllTeams();
    const fragment = document.createDocumentFragment();
    allTeams.forEach(club => {
        const option = document.createElement("option");
        option.value = club;
        fragment.append(option);
    });
    datalist.replaceChildren(fragment);
}

function getMatches() {
    if (matchesPromise == null) {
        matchesPromise = fetchJson('/getMatches').catch(error => {
            matchesPromise = null;
            throw error;
        });
    }
    return matchesPromise;
}

function getAllTeams() {
    if (allTeamsPromise == null) {
        allTeamsPromise = fetchJson('/getAllTeams').catch(error => {
            allTeamsPromise = null;
            throw error;
        });
    }
    return allTeamsPromise;
}

function getTeamData() {
    if (teamDataPromise == null) {
        teamDataPromise = fetchJson('/getTeamData').catch(error => {
            teamDataPromise = null;
            throw error;
        });
    }
    return teamDataPromise;
}

async function fetchJson(path) {
    try {
        const response = await fetch(window.location.origin + path);
        if (!response.ok) throw new Error('HTTP ' + response.status);
        return response.json();
    } catch (error) {
        alert("Es ist ein Fehler beim Laden aufgetreten: " + error);
        console.error('Error: ', error);
        throw error;
    }
}

function invalidateClubCache() {
    allTeamsPromise = null;
}

async function updateAllMatches() {
    const button = document.getElementById("updateMatchesButton");
    const status = document.getElementById("updateMatchesStatus");
    button.disabled = true;
    status.textContent = "Spiele werden von Fussball.de geladen ...";
    status.className = "settings-status-pending";
    try {
        const response = await fetch(window.location.origin + "/getNextMatches");
        if (!response.ok) throw new Error("HTTP " + response.status);
        const matches = await response.json();
        matchesPromise = Promise.resolve(matches);
        status.textContent = "Spiele erfolgreich aktualisiert · " + new Date().toLocaleTimeString("de-DE", {hour: "2-digit", minute: "2-digit"});
        status.className = "settings-status-success";
    } catch (error) {
        status.textContent = "Aktualisierung fehlgeschlagen. Bitte später erneut versuchen.";
        status.className = "settings-status-error";
        console.error("Error: ", error);
    } finally {
        button.disabled = false;
    }
}

async function updateTeamValues(teamInfo) {
    try {
        const response = await fetch(window.location.origin + '/updateTeamInfo', {
            method: 'POST',
            headers: {'Content-Type': 'application/json'},
            body: JSON.stringify(teamInfo)
        });
        if (!response.ok) throw new Error(await response.text());
        teamDataPromise = Promise.resolve(teamInfo);
        alert("Erfolgreich gespeichert!");
        return true;
    } catch (error) {
        alert("Es ist ein Fehler beim Ändern aufgetreten: " + error);
        console.error('Error: ', error);
        return false;
    }
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