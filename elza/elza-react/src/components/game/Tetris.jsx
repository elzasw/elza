import React, {Component} from 'react';
import ReactDOM from 'react-dom';
import lev from "./TetrisLevels";
import './Tetris.less';

const ww = 10;
const hh = 20;
var pw = 22;
var ph = 22;

const BRICK = 9;

const nextId = () => {return Math.ceil(Math.random() * 6)}

class Tetris extends Component {
    constructor(props) {
        super(props);

        const boardInfo = this.createBoard(0);

        this.state = {
            nextPieceId: null,
            board: boardInfo.board,
            bricksCount: boardInfo.bricksCount,
            lives: 5,
            pause: false,
            score: 0,
            end: true,
            endInfo: null,
        };
    }

    componentDidMount() {
    }

    componentWillUnmount() {
        this.clearTimer();
    }


    createBoard = (levelNum) => {
        const level = lev.levels[levelNum];
        let bricksCount = 0;

        const board = [];
        for (let y=0; y<hh; y++) {
            board[y] = [];
            for (let x=0; x<ww; x++) {
                board[y][x] = level[y * ww + x];
                if (board[y][x] === BRICK) {
                    bricksCount++;
                }
            }
        }

        return {board, bricksCount};
    };

    newGame = () => {
        const boardInfo = this.createBoard(0);

        this.setState({
            end: false,
            nextPieceId: nextId(),
            levelNum: 0,
            score: 0,
            lives: 5,
            board: boardInfo.board,
            bricksCount: boardInfo.bricksCount,
            levelInfo: {...lev.levelInfo[0]},
        }, () => {
            this.nextPiece();
        })
        this.changeSpeed(1000);
        ReactDOM.findDOMNode(this.refs.board).focus();
    };

    clearTimer = () => {
        if (this.timer) {
            clearTimeout(this.timer);
            this.timer = null;
        }
    };

    nextLevel = () => {
        const {score, levelNum} = this.state;
        const newLevelNum = levelNum + 1;

        if (newLevelNum >= lev.levels.length) {
            this.clearTimer();
            this.setState({
                end: true,
                endInfo: "Výborně, jsi vítěz!!!!!!"
            });
            return;
        }

        const boardInfo = this.createBoard(newLevelNum);
        this.setState({
            score: score + (5000 * newLevelNum),
            nextPieceId: nextId(),
            levelNum: newLevelNum,
            board: boardInfo.board,
            bricksCount: boardInfo.bricksCount,
            levelInfo: {...lev.levelInfo[newLevelNum]},
        }, () => {
            this.nextPiece();
            this.changeSpeed(1000 - (15 * newLevelNum));
        });
    };

    changeSpeed = (speed) => {
        const {pause} = this.state;

        this.setState({ speed })

        if (speed && !pause) {
            this.clearTimer();
            this.timer = setTimeout(this.tick, speed);
        }
    };

    tick = (doMove = true) => {
        doMove && this.movePieceDown();

        const {pause, speed} = this.state;
        if (speed && !pause) {
            this.clearTimer();
            this.timer = setTimeout(this.tick, speed);
        }
    };

    nextPiece = () => {
        const {piece, nextPieceId} = this.state;
        const id = nextPieceId;
        const newNextPieceId = nextId();

        const newPiece = {
            x: ww / 2 - Math.floor(lev.sizes[id] / 2),
            y: -lev.sizes[id],
            id: id,
            content: [...lev.pieces[id]]
        };

        const {board} = this.state;


        this.setState({ piece: newPiece, nextPieceId: newNextPieceId })
    };

    isCollision = (piece, tx, ty) => {
        const {board} = this.state;

        for (let y=0; y<lev.sizes[piece.id]; y++) {
            const yy = ty + y;

            for (let x=0; x<lev.sizes[piece.id]; x++) {
                const i = piece.content[y * lev.sizes[piece.id] + x];
                const xx = tx + x;

                if (i && (xx < 0 || xx >= ww || yy >= hh)) { // je mimo, je kolize
                    return true;
                }

                let boardi;
                if (yy < 0) {
                    boardi = 0;
                } else if (yy >= hh) {
                    boardi = 1;
                } else {
                    boardi = board[yy][xx];
                }

                if (i && boardi) {  // kolize
                    return true;
                }
            }
        }

        return false;
    };

    movePieceHorizontal = (rx) => {
        const {piece} = this.state;
        if (!this.isCollision(piece, piece.x + rx, piece.y)) {
            this.setState({
                piece: {
                    ...piece,
                    x: piece.x + rx
                }
            });
        }
    };

    placePiece = (board, piece) => {
        const linesToCheck = [];
        let linesInView = 0;

        for (let y=0; y<lev.sizes[piece.id]; y++) {
            let placed = false;
            const yy = piece.y + y;

            if (yy < 0) continue;

            for (let x=0; x<lev.sizes[piece.id]; x++) {
                const i = piece.content[y * lev.sizes[piece.id] + x];
                const xx = piece.x + x;
                if (i) {
                    placed = true;
                    board[yy][xx] = i;
                }
            }
            if (placed) {
                linesInView++;

                let isFull = true;
                for (let x = 0; x < ww; x++) {
                    if (!board[yy][x]) {
                        isFull = false;
                        break;
                    }
                }
                if (isFull) {
                    linesToCheck.push(yy);
                }
            }
        }

        return {linesToCheck, linesInView};
    };

    movePieceDown = () => {
        const {piece} = this.state;
        if (!piece) {
            return;
        }

        if (!this.isCollision(piece, piece.x, piece.y + 1)) {
            this.setState({
                piece: {
                    ...piece,
                    y: piece.y + 1
                }
            });
        } else {    // umístění dílku a nový
            const {lives, levelNum, score, bricksCount, board} = this.state;

            // Umístění dílku
            const {linesToCheck, linesInView} = this.placePiece(board, piece);

            const {levelInfo} = this.state;
            let newLevelInfo = {...levelInfo};

            // Pokud je dílek mimo obrazovku, končíme hru
            let linesScore = 0;
            if (linesInView) {  // umístěn na board
                let count = linesToCheck.length;
                while (true) {
                    if (count >= 4 && newLevelInfo.l4) {
                        count -= 4;
                        newLevelInfo.l4--;
                        linesScore += 100;
                    } else if (count >= 3 && newLevelInfo.l3) {
                        count -= 3;
                        newLevelInfo.l3--;
                        linesScore += 70;
                    } else if (count >= 2 && newLevelInfo.l2) {
                        count -= 2;
                        newLevelInfo.l2--;
                        linesScore += 40;
                    } else if (count >= 1 && newLevelInfo.l1) {
                        count -= 1;
                        newLevelInfo.l1--;
                        linesScore += 10;
                    } else {
                        break;
                    }
                }

                // Odebrání plných linek
                let removedBricks = 0;
                linesToCheck.forEach(ddy => {
                    for (let x = 0; x < ww; x++) {
                        if (board[ddy][x] === BRICK) {
                            removedBricks++;
                        }
                    }
                    for (let y=ddy; y>=0; y--) {
                        for (let x = 0; x < ww; x++) {
                            if (y > 0) {
                                board[y][x] = board[y - 1][x];
                            } else {
                                board[y][x] = 0;
                            }
                        }
                    }
                });

                // Nový stav
                const newBricksCount = bricksCount - removedBricks;
                this.setState({ board: board, bricksCount: newBricksCount, levelInfo: newLevelInfo, score: score + linesScore + (((levelNum + 1) * 50 * linesToCheck.length)) + 5 + (20 * removedBricks)});

                // Kontrola, zda již nedokončil level
                let doNextLevel = false;
                if (newBricksCount === 0) {
                    if (newLevelInfo.l1 === 0 && newLevelInfo.l2 === 0 && newLevelInfo.l3 === 0 && newLevelInfo.l4 === 0) {
                        doNextLevel = true;
                    }
                }

                this.setState({}, () => {
                    if (doNextLevel) {
                        this.nextLevel();
                    } else {
                        this.nextPiece();
                    }
                });
            } else {    // je mimo, konec hry, pokud nema jeste zivoty
                if (lives) {    // pokračujeme
                    this.setState({lives: lives - 1});
                    this.nextLevel();
                } else {    // opravdový konec hry
                    this.clearTimer();
                    this.setState({
                        piece: null,
                        speed: 0,
                        board: board,
                        end: true,
                        endInfo: "Hmmm, nic moc, prohrál jsi!!!!!!"
                    });
                }
            }
        }
    };

    rotatePiece = () => {
        const {piece} = this.state;
        const newPieceContent = [];

        for (let y=0; y<lev.sizes[piece.id]; y++) {
            for (let x=0; x<lev.sizes[piece.id]; x++) {
                newPieceContent[(lev.sizes[piece.id] - x - 1) * lev.sizes[piece.id] + y] = piece.content[y * lev.sizes[piece.id] + x];
            }
        }

        const newPiece = { ...piece, content: newPieceContent }

        if (!this.isCollision(newPiece, piece.x, piece.y)) {
            this.setState({ piece: newPiece });
        }
    };

    handleKeyDown = (e) => {
        const {piece} = this.state;

        if (!piece) {
            return;
        }

        switch (e.key) {
            case "ArrowLeft":
                this.movePieceHorizontal(-1);
                break;
            case "ArrowRight":
                this.movePieceHorizontal(+1);
                break;
            case "ArrowDown":
                this.movePieceDown();
                break;
            case "ArrowUp":
                this.rotatePiece();
                break;
        }
    };

    togglePause = () => {
        const {pause} = this.state;

        if (!pause) {
            this.clearTimer();
        }

        this.setState({
            pause: !pause
        }, () => {
            if (pause) {
                this.tick(false);
            }
        });

        ReactDOM.findDOMNode(this.refs.board).focus();
    };

    renderPoint = (x, y, i) => {
        if (i === BRICK) {
            return this.renderBrick(x, y);
        } else {
            return <rect fill={`url(#g${i})`} className={`item is${i}`} width={pw} height={ph} x={x * pw} y={y * ph}></rect>
        }
    };

    renderGradient = (i, color) => {
        return <radialGradient id={`g${i}`} cx="0.5" cy="0.5" r="1.4" fx="0.5" fy="0.5"><stop offset="0%" stopColor={color}/><stop offset="100%" stopColor="rgb(200, 200, 200)"/></radialGradient>;
    };

    renderGradients = () => {
        return <defs>
            {this.renderGradient(1, "blue")}
            {this.renderGradient(2, "purple")}
            {this.renderGradient(3, "cyan")}
            {this.renderGradient(4, "orange")}
            {this.renderGradient(5, "red")}
            {this.renderGradient(6, "green")}
            {this.renderGradient(7, "yellow")}
            <radialGradient id={`gb`} cx="0.5" cy="0.5" r="0.9" fx="0.5" fy="0.5"><stop offset="0%" stopColor="#a64413"/><stop offset="100%" stopColor="rgb(255, 0, 0)"/></radialGradient>
        </defs>
    };

    renderBrick = (x, y) => {
        const hx = pw / 2;
        const hy = ph / 2;

        return <g>
            <rect fill={`url(#gb)`} className={`brick`} width={pw} height={ph/2} x={x*pw} y={y*ph}></rect>
            <rect fill={`url(#gb)`} className={`brick`} width={pw/2} height={ph/2} x={x*pw} y={y*ph+ph/2} strokeDasharray={`${hx+hy+hx} ${hy}`}></rect>
            <rect fill={`url(#gb)`} className={`brick`} width={pw/2} height={ph/2} x={x*pw+pw/2} y={y*ph+ph/2} strokeDasharray={`${hx} ${hy} ${hx+hy}`}></rect>
        </g>
    };

    showHelp = () =>  {
        window.alert("Máš omezený počet životů, pokud se ti nepovede dokončit daný level, ztratíš jeden život a dostaneš se do dalšího levelu.\n\n" +
        "K dokončení levelu se musí splnit úkoly. Existují dva druhy úkolů: zničení všech cihel a zničení určitého počtu linek. Pokud je cílem levelu např. zničení typu 'Linky-4: 2', tak se musí celkem dvakrát zničit současně 4 linky jedním položením elementu (v tomto případě musí jít pouze o tyčku).\n\n" +
        "Hra se ovládá pomocí klávesových šipek.");
    }

    render() {
        const {endInfo, lives, score, bricksCount, levelInfo, levelNum, nextPieceId, end, pause, piece, board} = this.state;
        const items = [];

        for (let y=0; y<hh; y++) {
            for (let x=0; x<ww; x++) {
                const i = board[y][x];
                if (i) {
                    items.push(this.renderPoint(x, y, i));
                }
            }
        }

        // Dílek
        if (piece) {
            for (let y=0; y<lev.sizes[piece.id]; y++) {
                for (let x=0; x<lev.sizes[piece.id]; x++) {
                    const i = piece.content[y * lev.sizes[piece.id] + x];
                    if (i) {
                        items.push(this.renderPoint(piece.x + x, piece.y + y, i));
                    }
                }
            }
        }

        // Další dílek
        const info = [];
        if (nextPieceId !== null) {
            const off = Math.floor((5-lev.sizes[nextPieceId]) / 2)
            for (let y=0; y<lev.sizes[nextPieceId]; y++) {
                for (let x=0; x<lev.sizes[nextPieceId]; x++) {
                    const i = lev.pieces[nextPieceId][y * lev.sizes[nextPieceId] + x];
                    if (i) {
                        info.push(this.renderPoint(x + off, y + off + 1, i));
                    }
                }
            }
        }

        return (
            <div className="tetris" key={pw}>
                <div className="actions">
                    <button onClick={this.newGame}>{end ? "Nová hra" : "Restart hry"}</button>
                    <button disabled={end} onClick={this.togglePause}>{pause ? "Pokračovat" : "Pauza"}</button>
                    <button onClick={this.showHelp}>Nápověda</button>
                    {false && <button onClick={this.nextLevel}>Další level</button>}
                    &nbsp;&nbsp;&nbsp;&nbsp;Velikost zobrazení:&nbsp;<button onClick={() => { pw++; ph++; }}>+</button><button onClick={() => { if (pw > 2) {pw--; ph--;} } }>-</button>
                    <button onClick={this.props.onClose}>Zavřít</button>
                </div>
                {end && <h1>{endInfo}</h1>}
                {levelNum >= 0 && <div><b>Level {(levelNum+1)}/{lev.levels.length}</b></div>}
                <div className="game">
                    <svg className={"board" + (end ? " end" : "")} ref="board" tabIndex="0" onKeyDown={this.handleKeyDown} style={{width: ww * pw, height: hh * ph}} version="1.1" xmlns="http://www.w3.org/2000/svg">
                        {this.renderGradients()}
                        {items}
                    </svg>
                    <div className="info-container">
                        <svg className={"piece-info" + (end ? " end" : "")} style={{width: 5 * pw, height: 6 * ph}} version="1.1" xmlns="http://www.w3.org/2000/svg">
                            {info}
                        </svg>
                        {levelInfo && <div className="level-info">
                            <div><b>Score: {score}</b></div>
                            <br/>
                            <div>Životy: {lives}</div>
                            <br/>
                            <div>Linky-1: {levelInfo.l1}</div>
                            <div>Linky-2: {levelInfo.l2}</div>
                            <div>Linky-3: {levelInfo.l3}</div>
                            <div>Linky-4: {levelInfo.l4}</div>
                            <br/>
                            <div>Zničit cihel: {bricksCount}</div>
                        </div>}
                    </div>
                </div>
            </div>
        )
    }
}

export default Tetris;
